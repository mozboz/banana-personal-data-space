package requests

import actors.behaviors.{Response, Request}


case class GetNextAddress() extends Request
case class GetNextAddressResponse(request:Request) extends Response(request.messageId)
