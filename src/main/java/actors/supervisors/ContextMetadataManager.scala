package actors.supervisors

import actors.behaviors.BaseActor
import akka.actor.ActorRef
import requests.{Start,Stop}

class ContextMetadataManager extends BaseActor {

  def handleRequest = {
    case _ =>
  }

  def start(sender:ActorRef, message:Start) {
  }

  def stop(sender:ActorRef, message:Stop) {
  }
}