package requests

import java.util.UUID

import actors.behaviors.{Request, Response}

/**
 * Is sent by RequestHandlers to indicate that a request failed
 */
case class ErrorResponse(request:Request, ex : Exception = null) extends Response(request.messageId) {
  println("ErrorResponse: " + ex)
}
