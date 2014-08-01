package actors.supervisors

import actors.behaviors._
import akka.actor.{ActorRef, Actor}
import requests.{Start, Stop, ContextExistsResponse, ContextExists}


class ProfileActor extends Actor with Supervisor {

  def receive = handleResponse orElse {
    case x:Stop => handleShutdown(sender(), x)
    case x:ContextExists => handleContextExists(sender(), x)
  }

  def doStartup(sender:ActorRef, message:Start){

  }

  def doShutdown(sender:ActorRef, message:Stop) {

  }

  private def handleShutdown(sender:ActorRef, message:Stop) {
    // @todo: implement!
  }

  private def handleContextExists(sender:ActorRef, message:ContextExists) {
    sender ! ContextExistsResponse(message, exists = true)
  }
}