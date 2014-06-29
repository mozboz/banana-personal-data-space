package actors.supervisors

import akka.actor.Actor
import messages.{Add, Context, Item, Profile}

/**
 * The main endpoint of the application. Acts as a message broker.
 */
class EndpointActor extends Actor {
  def receive = {
    case Add(item:Context,to:Profile) => throw new Exception("Let it crash: " + item.getId + "->" + to.getUrl);
    case Add(item:Item,to:Context) => throw new Exception("Let it crash: " + item.getId + "->" + to.getId);
  }
}