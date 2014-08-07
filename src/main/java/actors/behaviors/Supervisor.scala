package actors.behaviors

import akka.actor.SupervisorStrategy.{Resume, Escalate}
import akka.actor.{OneForOneStrategy, Actor, ActorRef}
import requests._

import scala.collection.mutable

/**
 * Provides features to supervise other actors.
 */
trait Supervisor extends Actor with Aggregator
                               with RequestHandler
                               with Configurable {

  private val _supervisedActors = new mutable.HashMap[String,ActorRef]

  def handleSupervisorMessages: Receive =
    handleConfigurableMessages orElse
    new Receive {
    def isDefinedAt(x: Any) = x match {
      case x: Spawn => true
      case x: Kill => true
      case x: ListActors => true
      case _ => false
    }

    def apply(x: Any) = x match {
      case x: Spawn => handle[Spawn](sender(), x, spawn)
      case x: Kill => handle[Kill](sender(), x, kill)
      case x: ListActors => handle[ListActors](sender(), x, handleListActors)
      case _ => throw new Exception("This function is not applicable to objects of type: " + x.getClass)
    }
  }

  override val supervisorStrategy =
    OneForOneStrategy(3) {
      // @todo: Add meaningful exception handling
      case _: ArithmeticException => Resume
      case t => super.supervisorStrategy.decider.applyOrElse(t, (_: Any) => Escalate)
    }


  def getActor(id:String) : Option[ActorRef] = {
    _supervisedActors.get(id)
  }

  def spawn(sender: ActorRef, message: Spawn) {
    val actorRef = context.actorOf(message.props, message.id)
    _supervisedActors.put(message.id, actorRef)

    sender ! SpawnResponse(message, actorRef)
  }

  def kill(sender: ActorRef, message: Kill) {
    val actor = _supervisedActors.get(message.id).get
    context.stop(actor)
    _supervisedActors.remove(message.id)

    sender ! KillResponse(message)
  }

  def handleListActors(sender: ActorRef, message: ListActors) {
    sender ! ListActorsResponse(message, _supervisedActors)
  }

  override def handleStart(sender: ActorRef, message: Start) {
    // First start self, then the children
    start(sender, message,
      () => {
        notifyAllChildren(message,
          () => sender ! StartResponse(message),
          () => throw new Exception("Error while starting the children")
        )
      })
  }

  override def handleStop(sender: ActorRef, message: Stop) {
    // First stop the children, then self
    notifyAllChildren(message,
      () => stop(sender, message,
        () => sender ! StopResponse(message)),
      () => throw new Exception("Error while stopping the children")
    )
  }

  /**
   * Sends the specified request to all children and waits until all responded.
   * @param request The request
   * @param then The continuation which should be called when all responses arrived
   * @param error Timeout or other error continuation
   */
  def notifyAllChildren(request: Request,
                        then: () => Unit,
                        error: () => Unit) {
    notifySome(request, _supervisedActors.values, then, error)
  }

  /**
   * Sends the specified request to all children and aggregates every response that arrives.
   * @param request The request
   * @param aggregator The aggregator which gets the response and a "completed" function as parameters
   * @param then The continuation that should be executed when the aggregation finished
   * @param error The error continuation
   */
  def aggregateAllChildren(request: Request,
                           aggregator: (Response, ActorRef, () => Unit) => Unit,
                           then: () => Unit = () => {},
                           error: () => Unit = () => {}) {
    aggregateSome(request, _supervisedActors.values, aggregator, then, error)
  }
}