package actors.supervisors

import actors.behaviors.BaseActor
import akka.actor.ActorRef
import requests.{Stop, Start}

class TestFactory extends BaseActor {

  def start(sender:ActorRef, message:Start) {
    println("doStartup " + actorId)
  }

  def handleRequest = {
    case _ =>
  }

  def stop(sender:ActorRef, message:Stop) {
    println("doShutdown " + actorId)
  }
}
