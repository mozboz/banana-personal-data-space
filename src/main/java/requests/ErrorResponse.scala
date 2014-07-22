package requests

import actors.behaviors.Response

/**
 * Is sent by RequestHandlers to indicate that a request failed
 */
case class ErrorResponse(ex : Exception = null) extends Response
