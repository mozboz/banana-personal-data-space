package app

import akka.actor.{ActorSystem}

object BPDS extends App {

  implicit val system = ActorSystem("ProfileSystem")

}