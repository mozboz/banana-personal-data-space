package actors.supervisors

import actors.behaviors._
import akka.actor.{ActorRef, Actor}
import requests.{Shutdown, ContextExistsResponse, ContextExists}


class ProfileActor extends Actor with Requester { // @todo: add "with SystemEvents"

  def receive = handleResponse orElse {
    case x:Shutdown => handleShutdown(sender(), x)
    case x:ContextExists => handleContextExists(sender(), x)
  }

  private def handleShutdown(sender:ActorRef, message:Shutdown) {
    // @todo: implement!
  }

  private def handleContextExists(sender:ActorRef, message:ContextExists) {
    sender ! ContextExistsResponse(message, exists = true)
  }
}