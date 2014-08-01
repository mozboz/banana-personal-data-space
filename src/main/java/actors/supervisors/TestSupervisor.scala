package actors.supervisors

import actors.behaviors.BaseActor
import akka.actor.ActorRef
import requests.{Start,Stop}

class TestSupervisor extends BaseActor {

  def doStartup(sender:ActorRef, message:Start) {
    println("doStartup " + actorId)
  }

  def handleRequest = {
    case _ =>
  }

  def doShutdown(sender:ActorRef, message:Stop) {
    println("doShutdown " + actorId)
  }
}