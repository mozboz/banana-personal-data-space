package actors.behaviors

import java.util.UUID

import akka.actor.{ActorRef, Actor}
import requests.ErrorResponse

import scala.collection.mutable


trait RequestResponder2 {
  this: Actor  =>

  /**
   * Defines a partial Receive function which can be used by an actor.
   */
  def handleRequest: Receive = new Receive {

    def isDefinedAt(x: Any) = {
      x match  {
        case x:Request => true
        case _ => false
      }
    }

    def apply(x: Any) = {
      x match {
        case x: Request =>
          try {
            // @todo: Add timeout for the case that the response is never provided
            _pendingResponses.put(x.messageId, (response) => {
              sender ! response
            })

            // handler.apply(x)
          } catch {
            case e: Exception =>
              val errorResponse = new ErrorResponse(x, e)
              //errorResponse.setRequestId(x.messageId)
              sender ! errorResponse
          }

        case _ => throw new Exception("Can only be applied on Request-messages.");
      }
    }
  }

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
        val errorResponse = new ErrorResponse(x, e)
        //errorResponse.setRequestId(x.messageId)
        sender ! errorResponse
    }
  }

  def respond(x:Request, y:Response) {
    //y.setRequestId(x.messageId)
    _pendingResponses.get(x.messageId).get.apply(y)
    _pendingResponses.remove(x.messageId)
  }
}