package requests

import actors.behaviors.{Response, Request}
import akka.actor.{Actor, ActorRef, Props}

import scala.reflect.ClassTag

object Spawn {
  def apply[T <: Actor: ClassTag]() {
    Spawn(Props[T])
  }
}

case class Spawn(props:Props) extends Request
case class SpawnResponse(request:Request, actorRef:ActorRef) extends Response(request.messageId)
// @todo: Don't f****ng pass every Request with each answer