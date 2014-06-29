package actors.supervisors

import akka.actor.{ActorRef, Props, Actor}
import messages.{Add, Context, Item, Profile}

import scala.collection.mutable

/**
 * Holds all context's metadata and is the broker
 * for all messages concerning contexts.
 *
 * ContextActors are spawned on first access.
 */
class ProfileActor extends Actor {

  var _contextActors = new mutable.HashMap[String,ActorRef]

  def receive = {
    case "start" => {
      // Load the profiles metadata and configuration.
    }
    case "stop" => {
      // Persist the profile's metadata and configuration.
      // Notify all children (contexts) to do the same and collect the responses.
    }
    case "createContext" => {
      // Check metadata if the context already exists
      //  already existing -> throw exception
      //  not existing     -> create new entry in metadata and assign a storage location
    }
    case "readValue" => {
      // Check if there is already an actor which represents the context
      //  already existing -> send a request to this actor
      //  not existing     -> spawn a new actor and send the request to this actor
      getOrSpawnContext("key") ! "readValue"

    }
    case "writeValue" => {
      // Check if there is already an actor which represents the context
      //  already existing -> send a request to this actor
      //  not existing     -> spawn a new actor and send the request to this actor
    }
  }

  def getOrSpawnContext(key:String) : ActorRef = {
    if (isContextRunning(key))
      _contextActors.get(key).get
    else
      spawnContext(key).get
  }

  /**
   * Spawns a new context-actor
   * @param key The key of the context
   */
  def spawnContext(key:String) = {
    val contextActor = context.actorOf(Props[ContextActor])
    _contextActors.put(key, contextActor)
  }

  /**
   * Stops a running context actor
   * @param key The key of the context
   */
  def stopContext(key:String) = {
    val actorRef = _contextActors.get(key).get
    context.stop(actorRef)
  }

  /**
   * Checks if a context exists in the profile's metadata
   * @param key The key of the context
   */
  def contextExists(key:String) : Boolean = {
    return false;
  }

  /**
   * Checks if there is a running context actor
   * @param key The key of the context
   */
  def isContextRunning(key:String) : Boolean = {
    _contextActors.contains(key)
  }
}