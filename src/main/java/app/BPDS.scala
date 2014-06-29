package app

import actors.EndpointActor
import akka.actor.{Actor, ActorSystem, Props}
import messages.{Profile, Item, Context, Add}

object BPDS extends App {

  implicit val system = ActorSystem("ProfileSystem")
  val endpoint = system.actorOf(Props[EndpointActor], name = "EndpointActor")  // the local actor

  endpoint !  Add(item=Context("contextName name"), to=Profile("http://profile.daniel.de"))
  endpoint !  Add(item=Item("item name"), to=Context("contextName name"))
}
