package actors.behaviors

import akka.actor.Actor.Receive
import akka.actor.ActorRef
import requests.ErrorResponse


trait RequestHandler {
  /**
   * Replaces the default actor receive function.
   */
  def handleRequest: Receive

  /**
   * Wraps the request handler in a try-catch-block and sends an ErrorResponse in the case of an error.
   * @param sender The sender
   * @param message The message
   * @param handler The handler
   * @tparam TRequest The request message type
   */
  def handle[TRequest <: Request](sender:ActorRef, message:TRequest, handler:(ActorRef, TRequest) => Unit) {
    try {
      handler(sender, message)
    } catch {
      case x:Exception => sender ! ErrorResponse(message, x)
    }
  }
}
