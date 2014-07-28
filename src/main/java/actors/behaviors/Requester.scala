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
  //type ResponseContinuation = (Response, ActorRef, () => Unit) => Unit

  /**
   * Maps the request id to all waiting response continuations for the request
   */
  private val _waiting : mutable.HashMap[UUID, (Response, ActorRef, () => Unit) => Unit] = new mutable.HashMap[UUID, (Response, ActorRef, () => Unit) => Unit]()

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
   * Adds a onResponse continuation to the _waiting map.
   * @param requestId The id of the request
   * @param onResponse The response continuation
   */
  private def addWaiting(requestId:UUID, onResponse:(Response, ActorRef, () => Unit) => Unit) {
    _waiting.put(requestId, onResponse)
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
   * Curried function which is passed into the response handler.
   * @param requestId The request id
   */
  private def handled(requestId:UUID) () {
    stopListen(requestId)
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
    addWaiting(request.messageId, onResponse)
  }

  /**
   * Stops listening to the responses for the supplied request id.
   * @param requestId The request id.
   */
  def stopListen(requestId:UUID) {
    _waiting.remove(requestId)
  }
  
  /**
   * Issues a request and executes a continuation on its response.
   * @param request The request
   * @param to The receiver
   * @param sender The sender
   * @param onResponse The continuation
   */
  @Deprecated
  def onResponseOf(request: Request, to: ActorRef, sender:ActorRef, onResponse: (Response) => (Unit)) {
    addWaiting(request.messageId, (response, sender, handled) => {
      onResponse(response)
      handled()
    })
    to.tell(request, sender)
  }
}