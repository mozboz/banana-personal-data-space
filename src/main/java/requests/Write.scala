package requests

import actors.behaviors.{Response, Request}

/**
 * Writes a value to a context
 * @param key The key
 * @param value The value
 * @param toContext The context
 */
case class Write(key:String, value:String, toContext:String) extends Request
case class WriteResponse(request:Request) extends Response(request.messageId)// @todo: Change this back to messageId only to prevent sending the whole request with every answer