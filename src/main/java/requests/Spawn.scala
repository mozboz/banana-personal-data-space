package requests

import actors.behaviors.{Response, Request}
import akka.actor.{ActorRef, Props}

case class Spawn(props:Props, id:String) extends Request
case class SpawnResponse(request:Request, actorRef:ActorRef) extends Response(request.messageId)
// @todo: Don't f****ng pass every Request with each answer (but maybe sometimes (not always) it could be valuable, for retry etc...?! other ideas?)