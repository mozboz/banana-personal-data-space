package actors.supervisors

import akka.actor.{ActorRef, Actor}
import events.PropagateProfile
import requests.{ContextExistsResponse, UnexpectedErrorResponse, ContextExists, SpawnContext}

import scala.collection.mutable

/**
 * Owns a group of contexts.
 * * Interface
 *   Handles events:
 *   * PropagateProfile: Sets the profile which is responsible for this context group owner.
 *
 *   Responds to following requests:
 *   * SpawnContext: Spawns the requested context if possible and returns a actorRef
 *   * ContextExists: Proxies the received request to the profile actor and then responds with the result
 *
 *   Issues the following requests:
 *   *
 *
 *   Proxies the following requests:
 *   * ContextExists: Proxies the received request to the profile actor and then responds with the result
 */
class ContextGroupOwnerActor extends Actor with RequestResponseActor {

  val _managedContexts = new mutable.HashMap[String,ActorRef]
  var _profile : ActorRef = null

  def receive = {

    case x:PropagateProfile => _profile = x.profileRef

    case x:Request => handleRequest(x, sender(), {
        case x: SpawnContext =>
        case x: ContextExists =>
      })

    case x:Response => handleResponse(x)
  }


  /**
   * Checks if the supplied context is running and calls the yes-continuation
   * with the found actor reference else calls the no continuation
   * @param contextKey
   * @param yes
   * @param no
   */
  def contextRunning(contextKey:String,
                     yes:(ActorRef) => Unit,
                     no: () => Unit) {
    if (_managedContexts.contains(contextKey))
      yes(_managedContexts.get(contextKey).get)
    else
      no()
  }

  /**
   * Checks if the context with the supplied key exists and returns the
   * reference to the corresponding actor if possible
   * @param contextKey The key
   * @param yes yes-continuation with Some[ActorRef] or None[ActorRef]
   * @param no no-continuation without arguments
   * @param error error-continuation with exception argument
   */
  def contextExists(contextKey:String,
                    yes:(Option[ActorRef]) => Unit,
                    no: () => Unit,
                    error: (Exception) => Unit) {
    contextRunning(contextKey,
      (actorRef) => yes(Some[ActorRef](actorRef)),
      () => {
      onResponseOf(ContextExists(contextKey), _profile, {
        case ContextExistsResponse(true) => yes(None)
        case ContextExistsResponse(false) => no()
        case x: UnexpectedErrorResponse => error(new Exception("Error while checking if context " + contextKey + " exists."))
      })
    })
  }
}