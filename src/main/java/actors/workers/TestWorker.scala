package actors.workers

import actors.behaviors.BaseActor
import akka.actor.ActorRef
import requests.{Start,Stop}

class TestWorker extends BaseActor {

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