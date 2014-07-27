package actors.behaviors

import java.util.UUID
import akka.actor.{Actor, ActorRef}

import scala.collection.mutable

/**
 * Provides methods to extend an actor with the possibility to issue requests to other actors which
 * implement the Responder trait.
 */
trait Requester2 {
  this: Actor  =>

  private val _pendingRequests = new mutable.HashMap[UUID,(Response) => (Boolean)]

  /**
   * Defines a partial Receive function which can be used by an actor.
   */
  def handleResponse: Receive = new Receive {

    def isDefinedAt(x: Any) = {
      x match  {
        case x:Response => true
        case _ => false
      }
    }

    def apply(x: Any) = {
      x match {
        case x: Response =>
          val processed = _pendingRequests
            .getOrElse(x.getRequestId(), (x: Response) => false)
            .apply(x)

          if (processed)
            _pendingRequests.remove(x.getRequestId())

        case _ => throw new Exception("Can only be applied on Response-messages.");
      }
    }
  }

  def onResponseOf(message:Message, to:ActorRef, sender:ActorRef, onResponse:(Response) => (Unit)) {
    // @todo: Add timeout for the case that the response is never provided
    _pendingRequests.put(message.messageId, (x) =>  {
      onResponse.apply(x)
      true
    })

    to.tell(message, sender)
  }
}