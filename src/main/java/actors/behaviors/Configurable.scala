package actors.behaviors

import akka.actor.{Actor, ActorRef}
import requests._
import utils.BufferedResource

import scala.collection.mutable

/**
 * A configurable actor.
 */
trait Configurable extends Actor with Aggregator {

  private val _configActor = new BufferedResource[String, ActorRef]("Config")
  def config = _configActor

  private val _configurableActors = new mutable.HashSet[ActorRef]


  def handleAddConfigurable(sender:ActorRef, message:AddConfigurable) {
    try {
      message.children.foreach(a => _configurableActors.add(a))
    } catch {
      case x:Exception => sender ! ErrorResponse(message, x) // @todo: think about using STM for that purpose?!?
    }
    sender ! AddChildrenResponse(message)
  }

  def handleRemoveConfigurable(sender:ActorRef, message:RemoveConfigurable) {
    try {
      message.children.foreach(a => _configurableActors.remove(a))
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
    notifySome(request, _configurableActors, then, error)
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
    aggregateSome(request, _configurableActors, aggregator, then, error)
  }

  /**
   * When overridden, processes startup logic for the actor.
   */
  def doStartup(sender:ActorRef, message:Startup)

  /**
   * When overridden, processes shutdown logic for the actor.
   */
  def doShutdown(sender:ActorRef, message:Shutdown)


  /**
   * Processes doStartup, then notify all children.
   * When all children responded with StartupResponse, then
   * send a StartupResponse to the parent.
   */
  def handleStartupInternal(sender:ActorRef, message:Startup) {
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
  def handleShutdownInternal(sender:ActorRef, message:Shutdown) {
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
      (actor) => aggregateOne(ReadConfig(key), actor, (response,sender,done) => {
        value(response.asInstanceOf[ReadConfigResponse].value)
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
      (actor) => aggregateOne(WriteConfig(key, value), actor, (response,sender,done) => {
        success()
      }),
      (exception) => throw exception)
  }
}