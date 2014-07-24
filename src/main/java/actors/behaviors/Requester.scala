package actors.behaviors

import java.util.UUID
import akka.actor.ActorRef

import scala.collection.mutable

/**
 * Provides methods to extend an actor with the possibility to issue requests to other actors which
 * implement the Responder trait.
 */
trait Requester {
  private val _pendingRequests = new mutable.HashMap[UUID,(Response) => (Boolean)]

  def handleResponse(x:Response) {
    val processed = _pendingRequests
      .getOrElse(x.requestId, (x:Response) => false)
      .apply(x)

    if (processed)
      _pendingRequests.remove(x.requestId)
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
