package requests

import actors.behaviors.{Response, Request}

/**
 * Asks if a context exists
 * @param context The name of the context to check
 */
case class ContextExists(context:String) extends Request
case class ContextExistsResponse(request:Request, exists:Boolean) extends Response(request.messageId)// @todo: Change this back to messageId only to prevent sending the whole request with every answer