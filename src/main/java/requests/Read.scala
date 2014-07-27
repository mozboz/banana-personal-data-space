package requests

import actors.behaviors.{Response, Request}

/**
 * Reads a value from a context
 * @param key The key
 * @param fromContext The context
 */
case class Read(key:String, fromContext:String) extends Request
case class ReadResponse(request:Request, data:String)  extends Response(request.messageId)

case class ReadFromContext(key:String) extends Request