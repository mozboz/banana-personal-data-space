package actors.supervisors

import actors.behaviors.WorkerActor
import akka.actor.ActorRef
import requests.{Start,Stop}

class TestSupervisor extends WorkerActor {

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