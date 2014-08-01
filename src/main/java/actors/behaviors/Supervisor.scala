package actors.behaviors

import akka.actor.{Actor, ActorRef}
import requests._

import scala.collection.mutable

/**
 * Provides features to supervise other actors.
 */
trait Supervisor extends Actor with Aggregator
                               with RequestHandler {

  private val _supervisedActors = new mutable.HashSet[ActorRef]

  def handleSupervisorMessages: Receive = new Receive {
    def isDefinedAt(x: Any) = x match {
      case x: Spawn => true
      case x: Kill => true
      case x: Start => true
      case x: Stop => true
      case x: ListActors => true
      case _ => false
    }
    def apply(x: Any) = x match {
      case x: Spawn => handle[Spawn](sender(), x, handleSpawn)
      case x: Kill => handle[Kill](sender(), x, handleKill)
      case x: Start => handle[Start](sender(), x, handleStart)
      case x: Stop => handle[Stop](sender(), x, handleStop)
      case x: ListActors => handle[ListActors](sender(), x, handleListActors)
      case _ => throw new Exception("This function is not applicable to objects of type: " + x.getClass)
    }
  }

  def handleSpawn(sender:ActorRef, message:Spawn) {
    val actorRef = context.actorOf(message.props)
    sender ! SpawnResponse(message, actorRef)
  }

  def handleKill(sender:ActorRef, message:Kill) {
    sender ! KillResponse(message)
  }

  def handleListActors(sender:ActorRef, message:ListActors) {
  }


  /**
   * Processes doStartup, then notify all children.
   * When all children responded with StartupResponse, then
   * send a StartupResponse to the parent.
   */
  def handleStart(sender:ActorRef, message:Start) {
    start(sender, message)

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
  def handleStop(sender:ActorRef, message:Stop) {
    stop(sender, message)

    notifyAllChildren(message,
      () => sender ! StopResponse(message),
      () => throw new Exception("Error while stopping the children")
    )
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
    notifySome(request, _supervisedActors, then, error)
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
    aggregateSome(request, _supervisedActors, aggregator, then, error)
  }

  /**
   * When overridden, processes startup logic for the actor.
   */
  def start(sender:ActorRef, message:Start)

  /**
   * When overridden, processes shutdown logic for the actor.
   */
  def stop(sender:ActorRef, message:Stop)
}