package actors.supervisors

import actors.behaviors.BaseActor
import akka.actor.ActorRef
import requests.{Startup,Shutdown}

class ContextMetadataManager extends BaseActor {

  def handleRequest = {
    case _ =>
  }

  def doStartup(sender:ActorRef, message:Startup) {
  }

  def doShutdown(sender:ActorRef, message:Shutdown) {
  }
}