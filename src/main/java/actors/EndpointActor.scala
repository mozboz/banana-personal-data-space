package actors

import akka.actor.Actor
import messages.{Profile,Context,Item,Add}


class EndpointActor extends Actor {
  def receive = {
    case Add(item:Context,to:Profile) => throw new Exception("Let it crash: " + item.getId + "->" + to.getUrl);
    case Add(item:Item,to:Context) => throw new Exception("Let it crash: " + item.getId + "->" + to.getId);
  }
}