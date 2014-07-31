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
                         with Requester
                         with Aggregator{

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
   * Processes doStartup, then notify all children.
   * When all children responded with StartupResponse, then
   * send a StartupResponse to the parent.
   */
  private def handleStartupInternal(sender:ActorRef, message:Startup) {
    /*config.reset(None)
    config.set((a,loaded,c) => loaded(message.configRef))*/

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
}