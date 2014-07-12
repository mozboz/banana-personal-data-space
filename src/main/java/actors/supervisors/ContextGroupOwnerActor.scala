package actors.supervisors

import akka.actor.{Props, ActorRef, Actor}
import events.PropagateProfile
import requests._

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
class ContextGroupOwnerActor extends Actor with RequestResponseActor
                                           with MessageHandler  {

  val _managedContexts = new mutable.HashSet[String]
  val _runningContexts = new mutable.HashMap[String,ActorRef]
  var _profile : ActorRef = null

  def receive = {

    case x:PropagateProfile => _profile = x.profileRef

    case x:Request => handleRequest(x, sender(), {

        case x: SpawnContext => spawnContext(x.context,
          (actorRef) => respondTo(x, SpawnContextResponse(actorRef))
                        /* @todo: Send the ContextSpawned event */,
          (exception) => throwExFromMessage(x, "Error while starting the context " + x.context + "."))

        case x: ContextExists => contextExists(x.context,
          (actorRefOption) => respondTo(x, ContextExistsResponse(exists = true)),
          () => respondTo(x, ContextExistsResponse(exists = false)),
          (exception) => throwExFromMessage(x, "Error while checking if context " + x.context + " exists."))
      })

    case x:Response => handleResponse(x)
  }

  /**
   * Spawns a new context.
   * @param contextKey The key of the context to spawn
   * @param success actor successfully spawned continuation
   * @param error actor could not be started continuation
   */
  def spawnContext(contextKey:String,
                   success:(ActorRef) => Unit,
                   error:(Exception) => Unit) {
    try {
      val contextActorRef = context.system.actorOf(Props[ContextActor], contextKey)
      _runningContexts.put(contextKey, contextActorRef)
      success(contextActorRef)
    } catch {
      case e:Exception => error(e)
    }
  }

  /**
   * Checks if the supplied context is running and calls the yes-continuation
   * with the found actor reference else calls the no continuation
   * @param contextKey The key of the context
   * @param yes The yes-continuation
   * @param no The no-continuation
   */
  def contextRunning(contextKey:String,
                     yes:(ActorRef) => Unit,
                     no: () => Unit) {
    if (_runningContexts.contains(contextKey))
      yes(_runningContexts.get(contextKey).get)
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