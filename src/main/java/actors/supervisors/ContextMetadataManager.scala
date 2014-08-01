package actors.supervisors

import actors.behaviors.BaseActor
import akka.actor.ActorRef
import requests.{Start,Stop}

class ContextMetadataManager extends BaseActor {

  def handleRequest = {
    case _ =>
  }

  def doStartup(sender:ActorRef, message:Start) {
  }

  def doShutdown(sender:ActorRef, message:Stop) {
  }
}