package requests

import actors.supervisors.{Response, Request}

/**
 * Reads a value from a context
 * @param key The key
 * @param fromContext The context
 */
case class Read(key:String, fromContext:String) extends Request
case class ReadResponse(data:String, from:String) extends Response