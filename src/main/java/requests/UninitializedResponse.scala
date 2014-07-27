package requests

import actors.behaviors.{Request, Response}

/**
 * Message that contains the names of the event classes
 * which must be sent first in order to use this actor.
 * @param messages A list of case class names
 */
case class UninitializedResponse(request:Request, messages:List[String]) extends Response(request.messageId)
//@todo: Replace list of strings with a list of types
//@todo: Derive this response from a more general "error response" in order to unify the error handling on lower levels
