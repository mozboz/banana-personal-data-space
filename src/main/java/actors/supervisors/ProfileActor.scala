package actors.supervisors

import actors.behaviors._
import akka.actor.{ActorRef, Actor}
import requests.{Start, Stop, ContextExistsResponse, ContextExists}


class ProfileActor extends BaseActor {

  def handleRequest = handleResponse orElse {
    case x:ContextExists => handleContextExists(sender(), x)
  }

  def start(sender:ActorRef, message:Start){

  }

  def stop(sender:ActorRef, message:Stop) {

  }

  private def handleContextExists(sender:ActorRef, message:ContextExists) {
    sender ! ContextExistsResponse(message, exists = true)
  }
}