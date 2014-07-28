package actors.behaviors

import java.util.UUID
import akka.actor.{Actor, ActorRef}

import scala.collection.mutable

/**
 * Provides methods to extend an actor with the possibility to issue requests to other actors and
 * execute actions when the responses arrive.
 */
trait Requester extends Actor{

  private val _pendingRequests = new mutable.HashMap[UUID, (Response) => (Boolean)]

  /**
   * Defines a partial Receive function which can be used by an actor.
   */
  def handleResponse: Receive = new Receive {

    def isDefinedAt(x: Any) = {
      x match {
        case Response(requestId) =>  _pendingRequests.contains(requestId)
        case _ => false
      }
    }

    def apply(x: Any) = {
      x match {
        case Response(y) =>
          _pendingRequests.get(y).get
            .apply(x match { case x: Response => x})

          _pendingRequests.remove(y)

        case _ => throw new Exception("Can only be applied on Response-messages.");
      }
    }
  }

  // @todo: Add a possibility to wait for more than one response to a request
  // Either from different actors (for aggregation) or the same (for progress update)

  def onResponseOf(request: Request, to: ActorRef, sender:ActorRef, onResponse: (Response) => (Unit)) {
    // @todo: Add timeout for the case that the response is never provided
    _pendingRequests.put(request.messageId, (x) => {
      onResponse.apply(x)
      true
    })

    to.tell(request, sender)
  }
}