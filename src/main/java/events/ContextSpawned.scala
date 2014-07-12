package events

import actors.supervisors.Event
import akka.actor.ActorRef

/**
 * Notification sent by the context owner to notify everybody involved that
 * a new context was spawned.
 * @param key The key of the spawned context
 * @param actorRef The actor reference
 */
case class ContextSpawned(key:String,actorRef:ActorRef) extends Event
