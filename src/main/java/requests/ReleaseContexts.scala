package requests

import actors.behaviors.{Response, Request}

/**
 * Asks an actor to release the control over a context.
 * @param contextKey A list of context keys to manage
 */
case class ReleaseContexts(contextKey:List[String]) extends Request
case class ReleaseContextsResponse(request:Request, released:List[String])  extends Response(request.messageId) // @todo: Change this back to messageId only to prevent sending the whole request with every answer

