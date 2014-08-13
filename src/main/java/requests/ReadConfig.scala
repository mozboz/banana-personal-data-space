package requests

import java.util.UUID

import actors.behaviors.{Response, Request}

case class ReadConfig (key:String) extends Request
case class ReadConfigResponse (request:Request, value:Any) extends Response(request.messageId)
// @todo: Don't f****ng pass every Request with each answer

case class ReadActorConfig (actorId:UUID, key:String) extends Request
case class ReadActorConfigResponse (request:Request, value:Any) extends Response(request.messageId)