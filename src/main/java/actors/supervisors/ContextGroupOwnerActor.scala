package actors.supervisors

import actors.behaviors.{BaseActor, MessageHandler, Requester}
import akka.actor.{Props, ActorRef, Actor}
import akka.event.LoggingReceive
import events.{DisconnectProfile, ConnectProfile}
import requests._
import utils.BufferedResource

import scala.collection.mutable

/**
 * Is responsible to spawn and stop context actors on request.
 */
class ContextGroupOwnerActor extends BaseActor
                             with MessageHandler { // @todo: add "with SystemEvents"

  val _managedContexts = new mutable.HashSet[String]
  val _runningContexts = new mutable.HashMap[String,ActorRef]
  val _profileResource = new BufferedResource[String, ActorRef]("Profile")
  var _configActor : ActorRef = null

  def handleRequest = {
    case x: ConnectProfile =>  handleConnectProfile(sender(),x)
    case x: DisconnectProfile => handleDisconnectProfile(sender(), x)
    case x: ManageContexts => handleManageContexts(sender(), x)
    case x: ReleaseContexts => handleReleaseContexts(sender(), x)
    case x: SpawnContext => handleSpawnContext(sender(), x)
    case x: ContextExists => handleContextExists(sender(), x)
  }

  def doStartup(sender:ActorRef, message:Startup) {
    _configActor = message.configRef
  }

  def doShutdown(sender:ActorRef, message:Shutdown) {
    // @todo: Test if this is suitable
    var toStop = _runningContexts.size
    _runningContexts.foreach((a) => {
      onResponseOf(message, a._2, self, (response) => {

        _runningContexts.remove(a._1)

        toStop = toStop - 1
        if (toStop == 0) {
          sender ! ShutdownResponse(message)
        }
      })
    })
  }

  def handleConnectProfile(sender:ActorRef, message:ConnectProfile) {
    _profileResource.set((a,loaded,c) => loaded(message.profileRef))
  }

  def handleDisconnectProfile(sender:ActorRef, message:DisconnectProfile) {
    _profileResource.reset(None)
  }

  def handleManageContexts(sender:ActorRef, message:ManageContexts) {
    message.contexts.foreach((contextKey) => _managedContexts.add(contextKey))
    sender ! ManageContextsResponse(message, message.contexts)
  }

  def handleReleaseContexts(sender:ActorRef, message:ReleaseContexts) {
    // @todo: implement release contexts (which is the counterpart to ManageContexts)
  }

  def handleSpawnContext(sender:ActorRef, message:SpawnContext) {

    def handleSpawnedContext(context:ActorRef) {
      onResponseOf(Startup(_configActor),  context, self, {
        case x:StartupResponse => sender ! SpawnContextResponse(message, context)
        case x:ErrorResponse => sender ! ErrorResponse(message, x.ex)
      })
    }

    spawnContext(message.context,
      (actorRef) => handleSpawnedContext(actorRef),
      (exception) => throwExFromMessage(message, "Error while starting the context " + message.context + "." + exception.toString))
  }

  def handleContextExists(sender:ActorRef, message:ContextExists) {
    contextExists(message.context,
      (actorRefOption) => sender ! ContextExistsResponse(message, exists = true),
      () => sender ! ContextExistsResponse(message, exists = false),
      (exception) => throwExFromMessage(message, "Error while checking if context " + message.context + " exists: " + exception))
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
          onResponseOf(ContextExists(contextKey), profileActorRef, self, {
            case ContextExistsResponse(x, true) => yes(None)
            case ContextExistsResponse(x, false) => no()
            case x: ErrorResponse => error(new Exception("Error while checking if context " + contextKey + " exists.", x.ex))
          })
        },
        (exception) => error(exception))

        },
      error = (ex) => error(ex)
    )
  }
}