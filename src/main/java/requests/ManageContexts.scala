package requests

import java.util.UUID

import actors.behaviors.{Response, Request}

/**
 * Asks a manager actor to manage the specific contexts
 * @param contexts The keys of the contexts
 */
case class ManageContexts(contexts:List[String]) extends Request
case class ManageContextsResponse(request:Request, accepted:List[String]) extends Response(request.messageId)// @todo: Change this back to messageId only to prevent sending the whole request with every answer
