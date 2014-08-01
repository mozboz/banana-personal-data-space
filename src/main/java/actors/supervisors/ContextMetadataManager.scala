package actors.supervisors

import actors.behaviors.WorkerActor
import akka.actor.ActorRef
import requests.{Start,Stop}

class ContextMetadataManager extends WorkerActor {

  def handleRequest = {
    case _ =>
  }

  def start(sender:ActorRef, message:Start, started:() => Unit) {
  }

  def stop(sender:ActorRef, message:Stop, stopped:() => Unit) {
  }
}