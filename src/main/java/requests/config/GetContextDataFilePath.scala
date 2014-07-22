package requests.config

import actors.behaviors.{Response, Request}

case class GetContextDataFilePath(contextKey:String) extends Request
case class GetContextDataFilePathResponse(path:String) extends Response
