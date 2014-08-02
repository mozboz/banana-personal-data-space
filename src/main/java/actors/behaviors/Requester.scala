package actors.behaviors

import java.util.UUID
import akka.actor.{Actor, ActorRef}

import scala.collection.mutable

/**
 * Provides methods to extend an actor with the possibility to issue requests to other actors and
 * execute actions when the responses arrive.
 */
trait Requester extends Actor {

  /**
   * Represents a function which should be called on the arrival of a response to a previously
   * issued request.
   * Takes the following parameters:
   * * Response - the response message
   * * ActorRef - the sending actor
   * * HandledCallback - a callback which must be called when all responses have been received.
   */
  type ResponseContinuation = (Response, ActorRef, () => Unit) => Unit

  /**
   * Maps the request id to all waiting response continuations for the request
   */
  private val _waiting : mutable.HashMap[UUID, ResponseContinuation] = new mutable.HashMap[UUID, ResponseContinuation]()

  /**
   * Defines a partial Receive function which can be used by an actor.
   */
  def handleResponse: Receive = new Receive {
    def isDefinedAt(x: Any) = isAwaitedResponse(x)
    def apply(x: Any) = x match {
      case x:Response =>  processResponse(x, sender())
      case _ => throw new Exception("This function is not applicable to objects of type: " + x.getClass)
    }
  }

  /**
   * Checks if a message is an awaited response.
   * @param x The message
   * @return yes/no
   */
  private def isAwaitedResponse(x:Any) : Boolean = x match {
    case Response(requestId) =>  _waiting.contains(requestId)
    case _ => false
  }

  /**
   * Processes the received response and passes the sender to the processing continuation.
   * @param x The response message
   * @param sender The sender of the response
   */
  private def processResponse(x:Response, sender:ActorRef) {
    x match {
      case Response(requestId) =>
        _waiting.get(requestId).get
          .apply(x, sender, handled(requestId))
    }
  }

  /**
   * Stops listening to responses of the specified request.
   * @param requestId The request id
   */
  def handled(requestId:UUID) () {
    _waiting.remove(requestId)
  }

  /**
   * Registers a continuation which is executed whenever a response
   * to the issued request arrives. To stop handling the responses
   * of a specific request, the handled-callback must be called from
   * within the continuation.
   * To stop listen to specific responses earlier, the stopListen function can be called.
   * @param request The request message
   * @param onResponse The on-response continuation
   */
  def expectResponse(request:Request, onResponse:(Response, ActorRef, () => Unit) => Unit) {
    _waiting.put(request.messageId, onResponse)
  }
}