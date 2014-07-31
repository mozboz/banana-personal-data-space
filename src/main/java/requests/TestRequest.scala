package requests

import actors.behaviors.{Response, Request}

case class TestRequest(message:String) extends Request
case class TestResponse(request:Request, message:String) extends Response(request.messageId)