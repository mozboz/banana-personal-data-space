package events

import actors.behaviors.Event
import akka.actor.ActorRef

/**
 * Propagates the context owner to all involved parties
 * @param contextGroupOwnerRef An ActorRef to the ContextGroupOwner
 */
case class ConnectContextGroupOwner(contextGroupOwnerRef:ActorRef) extends Event
