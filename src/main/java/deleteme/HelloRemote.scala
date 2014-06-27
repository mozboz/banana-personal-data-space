package deleteme

import akka.actor.{ActorSystem, Props}


object HelloRemote extends App  {
  val system = ActorSystem("HelloRemoteSystem")
  val remoteActor = system.actorOf(Props(new RemoteActor[Int](new Guessable[Int](51))), name = "RemoteActor")
}

import akka.actor._

class RemoteActor[T](guess: Guessable[T]) extends Actor {

  def receive = {
    case msg: String =>
      println(s"RemoteActor received message '$msg'")
      sender ! "Hello from the RemoteActor"
    case msg: Guessable[T] =>
      sender ! guess.compare(msg)
  }
}
