package requests

import actors.behaviors.{Response, Request}
import akka.actor.ActorRef


case class AddReferenceTo(uri:String, actor:ActorRef) extends Request
case class AddReferenceToResponse(request:Request) extends Response(request.messageId)
// @todo: Don't f****ng pass every Request with each answer