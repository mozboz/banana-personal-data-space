package requests

import actors.behaviors.{Response, Request}

case class ListActors() extends Request
case class ListActorsResponse(message:Request) extends Response(message.messageId)