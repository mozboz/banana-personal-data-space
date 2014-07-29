package requests

import actors.behaviors.{Request,Response}
import akka.actor.ActorRef

case class Startup(configRef:ActorRef) extends Request
case class SetupResponse(request:Request) extends Response(request.messageId) // @todo: Change this back to messageId only to prevent sending the whole request with every answer
