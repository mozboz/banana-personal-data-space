package actors.supervisors

import java.util.UUID
import akka.actor.ActorRef
import requests.UnexpectedErrorResponse
import scala.collection.mutable


trait RequestResponseActor {
  private val _pendingRequests = new mutable.HashMap[UUID,(Response) => (Boolean)]
  private val _pendingResponses = new mutable.HashMap[UUID,(Response) => Unit]

  def handleResponse(x:Response) {
    val processed = _pendingRequests
      .getOrElse(x.requestId, (x:Response) => false)
      .apply(x)

    if (processed)
      _pendingRequests.remove(x.requestId)
  }

  def onResponseOf(message:Message, to:ActorRef, callback:(Response) => (Unit)) {
    // @todo: Add timeout for the case that the response is never provided
    _pendingRequests.put(message.messageId, (x) =>  {
      callback.apply(x)
      true
    })
    to ! message
  }

  def handleRequest(x: Request, sender:ActorRef, handler: (Request) => (Unit)) {
    try {
      // @todo: Add timeout for the case that the response is never provided
      _pendingResponses.put(x.messageId, (response) => {
        sender ! response
      })
      handler.apply(x)
    } catch {
      case e: Exception => {
        val errorResponse = new UnexpectedErrorResponse()
        errorResponse.setRequestId(x.messageId)
        sender ! errorResponse
      }
    }
  }

  def sendResponse(x:Request, y:Response) {
    y.setRequestId(x.messageId)
    _pendingResponses.get(x.messageId).get.apply(y)
    _pendingResponses.remove(x.messageId)
  }
}

trait Request extends  Message

trait Response extends Message {
  private var _requestId : UUID = null;

  def setRequestId(messageId:UUID) {
    _requestId = messageId
  }

  def requestId = _requestId
}