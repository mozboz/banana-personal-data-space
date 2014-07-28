package requests

import actors.behaviors.{Response, Request}

/**
 * A generic response which is sent when the caller wants to have a
 * confirmation that the receiver acknowledged the message.
 * @param request The original request
 */
case class Ack(request:Request) extends Response(request.messageId)