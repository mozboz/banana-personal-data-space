package requests.config

import actors.behaviors.{Response, Request}

case class GetContextDataFilePath(contextKey:String) extends Request
case class GetContextDataFilePathResponse(request:Request, path:String) extends Response(request.messageId)
