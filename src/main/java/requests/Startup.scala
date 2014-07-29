package requests

import actors.behaviors.{Request,Response}
import akka.actor.ActorRef

/**
 * Recursive call which will walk down trough all actors in the hierarchy
 * and encourages every actor to reload its configuration.
 * This can be sent more than once.
 * @param configRef A reference to the configuration actor
 */
case class Startup(configRef:ActorRef) extends Request
case class StartupResponse(request:Request) extends Response(request.messageId) // @todo: Change this back to messageId only to prevent sending the whole request with every answer
