package requests

import actors.behaviors.{Response, Request}


case class GetEntry() extends Request
case class GetEntryResponse(request:Request) extends Response(request.messageId)
