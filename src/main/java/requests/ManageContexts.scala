package requests

import actors.behaviors.{Response, Request}

/**
 * Asks a manager actor to manage the specific contexts
 * @param contexts The keys of the contexts
 */
case class ManageContexts(contexts:List[String]) extends Request
case class ManageContextsResponse(accepted:List[String]) extends Response
