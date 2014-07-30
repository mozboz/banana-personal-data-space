package actors.behaviors

import java.util.UUID

import akka.actor.{ActorRef, Actor}
import akka.event.LoggingReceive
import requests._
import utils.BufferedResource
import scala.collection.mutable

/**
 * Provides convenient access to the configuration system and handles the startup and shutdown procedure.
 */
abstract class BaseActor extends Actor
                         with Requester {

  private val _actorId = UUID.randomUUID()
  def actorId = _actorId

  /**
   * Implements the actors receive-function and routes the incoming messages
   * either to handleSystemEvents, handleResponse or handleRequest.
   * handleRequest can be used for user defined message handling code.
   * @return
   */
  def receive = LoggingReceive(
    handleSystemEvents orElse
    handleResponse orElse
    handleRequest
  )

  /**
   * Replaces the default actor receive function.
   */
  def handleRequest: Receive

  private val _config = new BufferedResource[String, ActorRef]("Config")
  def config = _config

  private val _children = new mutable.HashSet[ActorRef]

  // @todo: Add dependency management and discovery ->
  // @todo: Create a function which takes a list of Request objects and matches them against ever isDefinedAt to get a list of supported Requests.

  /**
   * Partial function which handles the configuration system messages.
   */
  def handleSystemEvents: Receive = new Receive {
    def isDefinedAt(x: Any) = x match {
      case x: Startup => true
      case x: Shutdown => true
      case x: AddChildren => true
      case x: RemoveChildren => true
      case _ => false
    }

    /*
     * Order for the requests should be as follows:
     * 1. AddChildren
     * 2. Startup (recursive)
     * 3. Shutdown (recursive)
     * 4. RemoveChildren
     */
    def apply(x: Any) = x match {
      case x: Startup => handleStartupInternal(sender(), x)
      case x: Shutdown => handleShutdownInternal(sender(), x)
      case x: AddChildren => handleAddChildren(sender(), x)
      case x: RemoveChildren => handleRemoveChildren(sender(), x)
      case _ => throw new Exception("This function can not be applied to a value of " + x.getClass)
    }
  }

  /**
   * When overridden, processes startup logic for the actor.
   */
  def doStartup(sender:ActorRef, message:Startup)

  /**
   * When overridden, processes shutdown logic for the actor.
   */
  def doShutdown(sender:ActorRef, message:Shutdown)

  private def handleAddChildren(sender:ActorRef, message:AddChildren) {
    try {
      message.children.foreach(a => _children.add(a))
    } catch {
      case x:Exception => sender ! ErrorResponse(message, x) // @todo: think about using STM for that purpose?!?
    }
    sender ! AddChildrenResponse(message)
  }

  private def handleRemoveChildren(sender:ActorRef, message:RemoveChildren) {
    try {
      message.children.foreach(a => _children.remove(a))
    } catch {
      case x:Exception => sender ! ErrorResponse(message, x) // @todo: think about using STM for that purpose?!?
    }
    sender ! RemoveChildrenResponse(message)
  }

  /**
   * Sends the specified request to all children and waits until all responded.
   * @param request The request
   * @param then The continuation which should be called when all responses arrived
   * @param error Timeout or other error continuation
   */
  def notifyAllChildren (request:Request,
                         then:() => Unit,
                         error:() => Unit) {
    notifySome(request, _children, then, error)
  }

  def notifyOne(request:Request,
                one:ActorRef,
                then: () => Unit,
                error:() => Unit) {
    notifySome(request, List(one), then, error)
  }

  /**
   * Sends the specified request to some actors and waits until all responded.
   * @param request The request
   * @param then The continuation which should be called when all responses arrived
   * @param error Timeout or other error continuation
   */
  def notifySome (request:Request,
                  some:Iterable[ActorRef],
                  then:() => Unit,
                  error:() => Unit) {
    val remaining = new mutable.HashSet[ActorRef]
    some.foreach(a => remaining.add(a))

    if (remaining.size == 0) {
      then()
    } else {
      expectResponse(request, (response, sender, completed) => {
        remaining.remove(sender)

        if (remaining.size == 0) { // @todo: add timeout
          completed()
          then()
        }
      })

      for(childActor <- remaining) {
        childActor ! request
      }
    }
  }

  /**
   * Sends the specified request to all children and aggregates every response that arrives.
   * @param request The request
   * @param aggregator The aggregator which gets the response and a "completed" function as parameters
   * @param then The continuation that should be executed when the aggregation finished
   * @param error The error continuation
   */
  def aggregateAllChildren(request:Request,
                           aggregator:(Response, ActorRef, () => Unit) => Unit,
                           then:() => Unit = () => {},
                           error:() => Unit = () => {}) {
    aggregateSome(request, _children, aggregator, then, error)
  }

  /**
   * Sends the specified request to the recipient and catch the arriving response
   * @param request The request
   * @param aggregator The aggregator which gets the response and a "completed" function as parameters
   * @param then The continuation that should be executed when the aggregation finished
   * @param error The error continuation
   */
  def aggregateOne(request:Request,
                    one:ActorRef,
                    aggregator:(Response, ActorRef, () => Unit) => Unit,
                    then:() => Unit = () => {},
                    error:() => Unit = () => {}) {

    // Wraps the aggregator in a way that 'done' is called implicitly if not done from
    // within the handler
    val aggregatorWrapper = (response:Response, actor:ActorRef, done:() => Unit) => {
      var alreadyDone = false
      val doneWrapper = () => {
        if (!alreadyDone) {
          done()
          alreadyDone = true
        }
      }
      aggregator(response, actor, doneWrapper)
      doneWrapper()
    }
    aggregateSome(request, List(one), aggregatorWrapper, then, error)
  }

  /**
   * Sends the specified request to some actors and aggregates every response that arrives.
   * @param request The request
   * @param aggregator The aggregator which gets the response and a "completed" function as parameters
   * @param then The continuation that should be executed when the aggregation finished
   * @param error The error continuation
   */
  def aggregateSome(request:Request,
                    some:Iterable[ActorRef],
                    aggregator:(Response, ActorRef, () => Unit) => Unit,
                    then:() => Unit = () => {},
                    error:() => Unit = () => {}) {
    val remaining = new mutable.HashSet[ActorRef]
    some.foreach(a => remaining.add(a))

    if (remaining.size == 0) {
      then.apply()
    } else {
      var cancelled = false

      expectResponse(request, (response, sender, completed) => {
        remaining.remove(sender)

        // Wrap the completed call into a function which sets a cancelled-flag
        val finishedAggregation = () => {
          completed()
          cancelled = true
        }

        aggregator(response, sender, finishedAggregation)

        if (remaining.size == 0 || cancelled) {  // @todo: add timeout
          if (!cancelled) // already called when cancelled
            completed()

          then.apply()
        }
      })

      for(childActor <- remaining) {
        childActor ! request
      }
    }
  }

  /**
   * Processes doStartup, then notify all children.
   * When all children responded with StartupResponse, then
   * send a StartupResponse to the parent.
   */
  private def handleStartupInternal(sender:ActorRef, message:Startup) {
    config.reset(None)
    config.set((a,loaded,c) => loaded(message.configRef))

    doStartup(sender, message)

    notifyAllChildren(message,
      () => sender ! StartupResponse(message),
      () => throw new Exception("Error while starting the children")
    )
  }

  /**
   * Processes doShutdown, then notify all children.
   * When all children responded with ShutdownResponse, then
   * send a ShutdownResponse to the parent.
   */
  private def handleShutdownInternal(sender:ActorRef, message:Shutdown) {
    doShutdown(sender, message)

    notifyAllChildren(message,
      () => sender ! ShutdownResponse(message),
      () => throw new Exception("Error while shutting down the children")
    )
  }

  /**
   * Tries to get a value from the config.
   * @param key The config key
   * @param value The value-continuation
   * @param error The error-continuation
   */
  def readConfig(key:String, value:Any => Unit, error:Exception => Unit) {
    config.withResource(
      (actor) => aggregateSome(ReadConfig(key), List(actor), (response,sender,done) => {
        value(response.asInstanceOf[ReadConfigResponse].value)
        done()
      }),
      (exception) => throw exception)
  }

  /**
   * Tries to write a value to the config.
   * @param key The config key
   * @param value The value to write
   * @param success The success-continuation
   * @param error The error-continuation
   */
  def writeConfig(key:String, value:Any, success:() => Unit, error:Exception => Unit) {
    config.withResource(
      (actor) => aggregateSome(WriteConfig(key, value), List(actor), (response,sender,done) => {
        success()
        done()
      }),
      (exception) => throw exception)
  }
}