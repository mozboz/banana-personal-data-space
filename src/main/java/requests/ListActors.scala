package requests

import actors.behaviors.{Response, Request}
import akka.actor.ActorRef

case class ListActors() extends Request
case class ListActorsResponse(message:Request, actorRefs:Iterable[ActorRef]) extends Response(message.messageId)