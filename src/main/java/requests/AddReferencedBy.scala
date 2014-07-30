package requests

import actors.behaviors.{Response, Request}
import akka.actor.ActorRef

case class AddReferencedBy(uri:String, actor:ActorRef) extends Request
case class AddReferencedByResponse(request:Request) extends Response(request.messageId)
// @todo: Don't f****ng pass every Request with each answer