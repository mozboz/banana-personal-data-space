package actors.behaviors

import akka.actor.{Props, Actor, ActorRef}
import requests._
import utils.BufferedResource

import scala.collection.mutable

/**
 * Provides features to supervise other actors.
 */
trait Supervisor extends Actor with Aggregator {

  private val _supervisedActors = new mutable.HashSet[ActorRef]

  def handleSupervisorMessages: Receive = new Receive {
    def isDefinedAt(x: Any) = x match {
      case x: Spawn => true
      case x: Kill => true
      case x: Start => true
      case x: Stop => true
      //case x: List => true
      case _ => false
    }
    def apply(x: Any) = x match {
      case x: Spawn => handleSpawn(sender(), x)
      case x: Kill => handleKill(sender(), x)
      case x: Start => handleStart(sender(), x)
      case x: Stop => handleStop(sender(), x)
      //case x: List => handleList(sender(), x)
      case _ => throw new Exception("This function is not applicable to objects of type: " + x.getClass)
    }
  }

  def handleSpawn(sender:ActorRef, message:Spawn) {
    try {
      sender ! SpawnResponse(message, context.actorOf(message.props))
    } catch {
      case x:Exception => sender ! ErrorResponse(message, x) // @todo: think about using STM for that purpose?!?
    }
  }

  def handleKill(sender:ActorRef, message:Kill) {
    try {
      sender ! KillResponse(message)
    } catch {
      case x:Exception => sender ! ErrorResponse(message, x) // @todo: think about using STM for that purpose?!?
    }
  }

  /**
   * Sends the specified request to all children and waits until all responded.
   * @param request The request
   * @param then The continuation which should be called when all responses arrived
   * @param error Timeout or other error continuation
   */
  def notifyAllConfigurable (request:Request,
                         then:() => Unit,
                         error:() => Unit) {
    notifySome(request, _supervisedActors, then, error)
  }

  /**
   * Sends the specified request to all children and aggregates every response that arrives.
   * @param request The request
   * @param aggregator The aggregator which gets the response and a "completed" function as parameters
   * @param then The continuation that should be executed when the aggregation finished
   * @param error The error continuation
   */
  def aggregateAllConfigurable(request:Request,
                           aggregator:(Response, ActorRef, () => Unit) => Unit,
                           then:() => Unit = () => {},
                           error:() => Unit = () => {}) {
    aggregateSome(request, _supervisedActors, aggregator, then, error)
  }

  /**
   * When overridden, processes startup logic for the actor.
   */
  def doStartup(sender:ActorRef, message:Start)

  /**
   * When overridden, processes shutdown logic for the actor.
   */
  def doShutdown(sender:ActorRef, message:Stop)


  /**
   * Processes doStartup, then notify all children.
   * When all children responded with StartupResponse, then
   * send a StartupResponse to the parent.
   */
  def handleStart(sender:ActorRef, message:Start) {
    /*config.reset(None)
    config.set((a,loaded,c) => loaded(message.configRef))*/

    doStartup(sender, message)

    notifyAllConfigurable(message,
      () => sender ! StartupResponse(message),
      () => throw new Exception("Error while starting the children")
    )
  }

  /**
   * Processes doShutdown, then notify all children.
   * When all children responded with ShutdownResponse, then
   * send a ShutdownResponse to the parent.
   */
  def handleStop(sender:ActorRef, message:Stop) {
    doShutdown(sender, message)

    notifyAllConfigurable(message,
      () => sender ! StopResponse(message),
      () => throw new Exception("Error while shutting down the children")
    )
  }
}