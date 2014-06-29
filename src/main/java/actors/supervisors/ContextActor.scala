package actors.supervisors

import akka.actor.Actor
import model.Context

import scala.collection.mutable

/**
 * Represents one specific context. Holds a instance of the model's Context class
 * and is soley responsible for its persistence and access to it.
 */
class ContextActor extends Actor {

  def receive = {
    case "setup" => {
      // Configure this actor. Necessary step before sending any other message.
    }
    case "load" => {
      // Load a context from its storage location
    }
    case "persist" => {
      // Save a context to its storage location
    }
    case "readValue" => {
      // Read a value from the context
    }
    case "writeValue" => {
      // Write a value to the context
    }
  }
}