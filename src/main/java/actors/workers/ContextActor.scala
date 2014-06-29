package actors.workers

import akka.actor.Actor
import messages.control.{StopContext, StartContext}
import messages.data.{WriteValue, ReadValue}

/**
 * Represents one specific context. Holds a instance of the model's Context class
 * and is soley responsible for its persistence and access to it.
 */
class ContextActor extends Actor {

  def receive = {
    case x:StartContext => {
      // Configure this actor. Necessary step before sending any other message.
    }
    case x:StopContext => {
      // Save a context to its storage location
    }
    case x:ReadValue => {
      // Read a value from the context
    }
    case x:WriteValue => {
      // Write a value to the context
    }
  }
}