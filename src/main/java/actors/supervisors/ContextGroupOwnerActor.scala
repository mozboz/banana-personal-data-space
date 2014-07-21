package actors.supervisors

import actors.behaviors.{MessageHandler, Requester, RequestResponder, Request, Response}
import actors.workers.ContextActor
import akka.actor.{Props, ActorRef, Actor}
import akka.event.Logging
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
class ContextGroupOwnerActor extends Actor with Requester
                                           with RequestResponder
                                           with MessageHandler {
  val log = Logging(context.system, this)
  setLoggingAdapter(log)

  val _managedContexts = new mutable.HashSet[String]
  val _runningContexts = new mutable.HashMap[String,ActorRef]
  var _profile : ActorRef = null

  def receive = {
    case x:PropagateProfile =>
      _profile = x.profileRef

    case x:Request => handleRequest(x, sender(), {

        case x: ManageContexts =>
          x.contexts.foreach((contextKey) => _managedContexts.add(contextKey))
          respond(x, ManageContextsResponse(x.contexts))

        case x: ReleaseContexts =>
          // @todo: implement release contexts

        case x: SpawnContext =>
          log.debug("spawnContext(contextKey:" + x.context+ ")")
          spawnContext(x.context,
            (actorRef) => respond(x, SpawnContextResponse(actorRef)),
            (exception) =>
              throwExFromMessage(x, "Error while starting the context " + x.context + "." + exception.toString))

        case x: ContextExists =>
          contextExists(x.context,
            (actorRefOption) =>
              respond(x, ContextExistsResponse(exists = true)),
            () =>
              respond(x, ContextExistsResponse(exists = false)),
            (exception) =>
              throwExFromMessage(x, "Error while checking if context " + x.context + " exists: " + exception))
      })

    case x:Response => handleResponse(x)
  }

  def contextManagedByActor(contextKey:String,
                               yes:() => Unit,
                               no: () => Unit) {
    if (_managedContexts.contains(contextKey))
      yes()
    else
      no()
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
      contextManagedByActor(contextKey = contextKey,
        yes = () => {
            log.debug("ContextGroupOwner.spawnContext(contextKey:" + contextKey + ")")
            val contextActorRef = context.system.actorOf(Props[ContextActor], contextKey)
            _runningContexts.put(contextKey, contextActorRef)
            success(contextActorRef)
        },
        no = () => error(new Exception("The context with the key " + contextKey + " is not managed by this actor."))
      )
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
                     no: () => Unit,
                     error: (Exception) => Unit) {
    contextManagedByActor(contextKey,
      yes = () => {
        if (_runningContexts.contains(contextKey))
          yes(_runningContexts.get(contextKey).get)
        else
          no()
      },
      no = () => error(new Exception("The context with the key " + contextKey + " is not managed by this actor.")))
  }

  /**
   * Checks if the context with the supplied key exists and returns the
   * reference to the corresponding actor if possible
   * @param contextKey The key
   * @param yes yes-continuation with Some[ActorRef] or None
   * @param no no-continuation without arguments
   * @param error error-continuation with exception argument
   */
  def contextExists(contextKey:String,
                    yes:(Option[ActorRef]) => Unit,
                    no: () => Unit,
                    error: (Exception) => Unit) {
    contextRunning(
      contextKey = contextKey,
      yes = (actorRef) => yes(Some[ActorRef](actorRef)),
      no = () => {
        onResponseOf(ContextExists(contextKey), _profile, context.self, {
          case ContextExistsResponse(true) =>
            yes(None)
          case ContextExistsResponse(false) =>
            no()
          case x: UnexpectedErrorResponse => error(new Exception("Error while checking if context " + contextKey + " exists."))
        })},
      error = (ex) => error(ex)
    )
  }
}