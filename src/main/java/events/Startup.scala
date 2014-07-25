package events

import akka.actor.ActorRef

case class Startup(configActor:ActorRef)
