package requests

import java.net.URL

import actors.behaviors.{Response, Request}
import akka.actor.ActorRef

case class AggregateContext(uri:String, actor:ActorRef) extends Request
case class AggregateContextResponse(request:Request) extends Response(request.messageId)
// @todo: Don't f****ng pass every Request with each answer