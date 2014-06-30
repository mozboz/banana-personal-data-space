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
      // Check if the key exists
      //  if not -> return an error

      // Check if the context's data is loaded (or at least the requested key)
      //  if not -> load it

      // Return the requested data
    }
    case x:WriteValue => {
      // Check if the key exists
      //  if not -> create
      //  else   -> update
    }
  }
}