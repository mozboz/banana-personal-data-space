package requests

import actors.behaviors.{Request, Response}
import akka.actor.ActorRef

/**
 * Asks if a context could be spawned
 * @param context The name of the context to spawn
 */
case class SpawnContext(context:String) extends Request
case class SpawnContextResponse(request:Request, actorRef:ActorRef) extends Response(request.messageId)