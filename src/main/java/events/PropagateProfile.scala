package events

import actors.behaviors.Event
import akka.actor.ActorRef

/**
 * Propagates the profile to all involved parties
 * @param profileRef
 */
case class PropagateProfile(profileRef:ActorRef) extends Event
