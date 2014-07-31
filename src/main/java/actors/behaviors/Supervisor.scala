package actors.behaviors

import akka.actor.ActorRef
import scala.collection.mutable

trait Supervisor {
  private val _children = new mutable.HashSet[ActorRef]
}
