package requests

import actors.supervisors.{Response, Request}

/**
 * Writes a value to a context
 * @param key The key
 * @param value The value
 * @param toContext The context
 */
case class Write(key:String, value:String, toContext:String) extends Request
case class WriteResponse() extends Response