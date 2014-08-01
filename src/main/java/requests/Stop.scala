package requests

import actors.behaviors.{Response, Request}
import akka.actor.ActorRef


case class Stop(actorRef:ActorRef) extends Request
case class StopResponse(request:Request)  extends Response(request.messageId)// @todo: Change this back to messageId only to prevent sending the whole request with every answer