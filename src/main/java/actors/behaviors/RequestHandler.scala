package actors.behaviors

import akka.actor.Actor.Receive
import akka.actor.ActorRef
import requests.ErrorResponse


trait RequestHandler {
  /**
   * Replaces the default actor receive function.
   */
  def handleRequest: Receive

  def handle[TRequest <: Request](sender:ActorRef, message:TRequest, handler:(ActorRef, TRequest) => Unit) {
    try {
      handler(sender, message)
    } catch {
      case x:Exception => sender ! ErrorResponse(message, x)
    }
  }
  
  def throwExFromMessage(m:Message, additional:String = "") {
    throw new Exception("Error while processing the message with Id '" + m.messageId + "', Type '" + m.getClass.getName + "': " + additional)
  }
}
