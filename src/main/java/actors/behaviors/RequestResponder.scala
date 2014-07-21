package actors.behaviors

import java.util.UUID

import akka.actor.ActorRef
import requests.UnexpectedErrorResponse

import scala.collection.mutable

/**
 * Provides methods to extend an actor with the possibility to respond to requests from other actors
 * which implement the Requester trait.
 */
trait RequestResponder {
  private val _pendingResponses = new mutable.HashMap[UUID,(Response) => Unit]

  def handleRequest(x: Request, sender:ActorRef, handler: (Request) => (Unit)) {
    try {
      // @todo: Add timeout for the case that the response is never provided
      _pendingResponses.put(x.messageId, (response) => {
        sender ! response
      })

      handler.apply(x)
    } catch {
      case e: Exception =>
        val errorResponse = new UnexpectedErrorResponse(e)
        errorResponse.setRequestId(x.messageId)
        sender ! errorResponse
    }
  }

  def respond(x:Request, y:Response) {
    y.setRequestId(x.messageId)
    _pendingResponses.get(x.messageId).get.apply(y)
    _pendingResponses.remove(x.messageId)
  }
}