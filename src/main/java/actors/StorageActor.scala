package actors

import akka.actor.Actor
import messages.{Profile,Context,Item,Add}

/**
 * Manages the access to the data model. Only one instance allowed to avoid explicit synchronization
 * in the underlying data model.
 */
class StorageActor extends Actor {
  def receive = {
    case Add(item:Context,to:Profile) => throw new Exception("Let it crash: " + item.getId + "->" + to.getUrl);
    case Add(item:Item,to:Context) => throw new Exception("Let it crash: " + item.getId + "->" + to.getId);
  }
}