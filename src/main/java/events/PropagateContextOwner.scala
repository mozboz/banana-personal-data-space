package events

import actors.supervisors.Event
import akka.actor.ActorRef

/**
 * Propagates the context owner to all involved parties
 * @param contextOwnerRef
 */
case class PropagateContextOwner(contextOwnerRef:ActorRef) extends Event
