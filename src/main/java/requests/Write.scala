package requests

import java.nio.ByteBuffer

import actors.behaviors.{Response, Request}

/**
 * Writes a value to a context
 * @param key The key
 * @param value The value
 * @param toContext The context
 */
case class Write(key:String, value:String, toContext:String) extends Request
case class WriteResponse(request:Request) extends Response(request.messageId)// @todo: Change this back to messageId only to prevent sending the whole request with every answer

sealed abstract class FileMessage(offset:Long, length:Int) extends Request {
  val _offset = offset
  def getOffset = _offset

  val _length = length
  def getLength = _length
}

case class ReadFile(offset:Int, length:Int) extends FileMessage(offset, length)
case class ReadFileResponse(request:Request, data:ByteBuffer) extends Response(request.messageId)

case class WriteFile(offset:Long, data:ByteBuffer) extends FileMessage(offset, data.capacity())
case class WriteFileResponse(request:Request) extends Response(request.messageId)

case class FlushFile() extends Request
case class FlushFileResponse(request:Request) extends Response(request.messageId)