package requests

import actors.behaviors.{Response, Request}


case class Shutdown() extends Request
case class ShutdownResponse(request:Request)  extends Response(request.messageId)// @todo: Change this back to messageId only to prevent sending the whole request with every answer