package deleteme

import akka.actor.{ActorSystem, Props}

/**
 * Created with IntelliJ IDEA.
 * User: james
 * Date: 27/06/14
 * Time: 21:01
 * To change this template use File | Settings | File Templates.
 */
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
