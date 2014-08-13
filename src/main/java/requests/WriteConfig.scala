package requests

import java.util.UUID

import actors.behaviors.{Response, Request}


case class WriteConfig (key:String, value:Any) extends Request
case class WriteConfigResponse (request:Request) extends Response(request.messageId)
// @todo: Don't f****ng pass every Request with each answer


case class WriteActorConfig (actorId:UUID, key:String, value:Any) extends Request
case class WriteActorConfigResponse (request:Request) extends Response(request.messageId)