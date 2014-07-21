package events

import actors.behaviors.Event

/**
 * Notification sent by the context owner to notify everybody involved that
 * a context was stopped.
 * @param key The key of the stopped context
 */
case class ContextStopped(key:String) extends Event