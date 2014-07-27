package actors.supervisors

import actors.behaviors.{MessageHandler, Requester, Request}
import akka.actor.{Props, ActorRef, Actor}
import akka.event.LoggingReceive
import events.{DisconnectProfile, ConnectProfile}
import requests._
import utils.BufferedResource

import scala.collection.mutable

/**
 * Is responsible to spawn and stop context actors on request.
 */
class ContextGroupOwnerActor extends Actor with Requester
                                           //with RequestResponder
                                           with MessageHandler {

  val _managedContexts = new mutable.HashSet[String]
  val _runningContexts = new mutable.HashMap[String,ActorRef]

  var _profileResource = new BufferedResource[String, ActorRef]("Profile")
  var _configActor : ActorRef = null

  def receive = LoggingReceive(handleResponse orElse {
    
    case x:ConnectProfile =>  _profileResource.set((a,loaded,c) => loaded(x.profileRef))
    case x:DisconnectProfile => _profileResource.reset(None)

    case x:Setup => _configActor = x.configActor
    case x:Shutdown => _runningContexts.foreach((a) => a._2 ! x) // @todo: Remove from _runningContexts when stopped (request/response for shutdown)

    case x:ManageContexts =>
      x.contexts.foreach((contextKey) => _managedContexts.add(contextKey))
      sender ! ManageContextsResponse(x, x.contexts)

    //case x:Request => handleRequest(x, sender(), {

      /*  case x: ManageContexts =>
          x.contexts.foreach((contextKey) => _managedContexts.add(contextKey))
          respond(x, ManageContextsResponse(x.contexts))
*/
        case x: ReleaseContexts =>
          // @todo: implement release contexts

        case x: SpawnContext =>
          spawnContext(x.context,
            (actorRef) =>
              //actorRef ! // @todo: Send the configuration to the actor (as event?!)
              sender ! SpawnContextResponse(x, actorRef),
            (exception) => throwExFromMessage(x, "Error while starting the context " + x.context + "." + exception.toString))

        case x: ContextExists =>
          contextExists(x.context,
            (actorRefOption) => sender ! ContextExistsResponse(x, exists = true),
            () => sender ! ContextExistsResponse(x, exists = false),
            (exception) => throwExFromMessage(x, "Error while checking if context " + x.context + " exists: " + exception))
     // })

    //case x:Response => handleResponse(x)
  })

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
      yes = (actorRef) => yes(Some(actorRef)),
      no = () => {
        _profileResource.withResource((profileActorRef) => {
          onResponseOf(ContextExists(contextKey), profileActorRef, context.self, {
            case ContextExistsResponse(x, true) =>
              yes(None)
            case ContextExistsResponse(x, false) =>
              no()
            case x: ErrorResponse => error(new Exception("Error while checking if context " + contextKey + " exists.", x.ex))
          })
        },
        (exception) => error(exception))

        },
      error = (ex) => error(ex)
    )
  }
}