package requests

import actors.behaviors.{Response, Request}


case class Shutdown() extends Request
case class ShutdownResponse(request:Request)  extends Response(request.messageId)