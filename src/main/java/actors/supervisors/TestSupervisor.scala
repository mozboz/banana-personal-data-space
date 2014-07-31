package actors.supervisors

import actors.behaviors.BaseActor
import akka.actor.ActorRef
import requests.{Startup,Shutdown}

class TestSupervisor extends BaseActor {

  def doStartup(sender:ActorRef, message:Startup) {
    println("doStartup " + actorId)
  }

  def handleRequest = {
    case _ =>
  }

  def doShutdown(sender:ActorRef, message:Shutdown) {
    println("doShutdown " + actorId)
  }
}