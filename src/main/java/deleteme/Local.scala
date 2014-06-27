package deleteme

import akka.actor.{Props, ActorSystem}

/**
 * Created with IntelliJ IDEA.
 * User: james
 * Date: 27/06/14
 * Time: 21:00
 * To change this template use File | Settings | File Templates.
 */
object Local extends App {

  implicit val system = ActorSystem("LocalSystem")
  val localActor = system.actorOf(Props[LocalActor], name = "LocalActor")  // the local actor
  localActor ! "START"                                                     // start the action

}
