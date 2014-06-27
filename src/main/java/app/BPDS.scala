package app

import akka.actor.{Props, ActorSystem}
import actors._
import deleteme.Guessable

class BPDS extends App {

  val system = ActorSystem("YoDawgIHeardYouLikeActorsSoIPutSomeActorsInYourActors")
  val masterActor = system.actorOf(Props[MasterActor], name = "MasterActor")  // the local actor
  masterActor ! Action.ADD

  /*
  def receive = {
    case msg: String =>
      println(msg)
      // sender ! "Hello from the RemoteActor"
    // case _ => null
  }
    */
}
