package events

import actors.behaviors.Event


case class ErrorEvent(message:String) extends Event
