package requests

import actors.behaviors.{Response, Request}

/**
 * Asks an actor to release the control over a context.
 * @param contextKey A list of context keys to manage
 */
case class ReleaseContexts(contextKey:List[String]) extends Request
case class ReleaseContextsResponse(released:List[String]) extends Response

