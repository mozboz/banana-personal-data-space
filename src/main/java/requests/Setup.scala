package requests

import actors.behaviors.{Request,Response}
import akka.actor.ActorRef

case class Setup(configActor:ActorRef) extends Request
case class SetupResponse(request:Request) extends Response(request.messageId)
