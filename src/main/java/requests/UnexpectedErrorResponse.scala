package requests

import actors.supervisors.Response

/**
 * Is sent by RequestHandlers to indicate that a request failed
 */
case class UnexpectedErrorResponse() extends Response
