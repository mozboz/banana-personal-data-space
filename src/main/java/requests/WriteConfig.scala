package requests

import actors.behaviors.{Response, Request}


case class WriteConfig (key:String, value:Any) extends Request
case class WriteConfigResponse (request:Request) extends Response(request.messageId)
// @todo: Don't f****ng pass every Request with each answer
