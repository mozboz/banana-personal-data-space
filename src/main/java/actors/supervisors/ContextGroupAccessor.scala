package actors.supervisors

import akka.actor.{ActorRef, Actor}
import events.{PropagateContextOwner, PropagateProfile, ContextStopped, ContextSpawned}
import requests._
import scala.collection.mutable

/**
 * Actor which provides access to a group of context actors.
 * * Interface
 *   Handles events:
 *   * ContextSpawned: Makes this accessor responsible for the spawned context.
 *   * ContextStopped: Releases this accessor from the responsibility to manage access to this context actor.
 *   * PropagateProfile: Sets the profile which is responsible for this accessor.
 *   * PropagateContextOwner: Sets the context owner which is responsible for this accessor.
 *
 *   Responds to following requests:
 *   * Read: forwards the read request to a matching actor (if any, else returns error) and responds with its result
 *   * Write:
 *
 *   Issues the following requests:
 *   * SpawnContext:
 *   * ContextExists:
 *
 *   Proxies the following requests:
 *   * Read: forwards every read request which arrives to the context actor and then proxies the reply back to the caller.
 *   * Write:
 */
class ContextGroupAccessor extends Actor with RequestResponseActor
                                         with MessageHandler {

  val _managedContexts = new mutable.HashMap[String,ActorRef]

  var _contextGroupOwner : ActorRef = null
  var _profile : ActorRef = null

  def receive = {

    // Handle system events
    case x:ContextSpawned => handleContextSpawned(x.key, x.actorRef)
    case x:ContextStopped => _managedContexts.remove(x.key)

    case x:PropagateProfile => _profile = x.profileRef
    case x:PropagateContextOwner => _contextGroupOwner = x.contextOwnerRef

    // Handles all responses to previously issued requests
    case x:Response =>
      // @todo:Notify sender about the exact operation that failed because of the uninitialized state
      maySendUninitializedAndThrowException()
      handleResponse(x)


    case x:Request =>
      // @todo:Notify sender about the exact operation that failed because of the uninitialized state
      maySendUninitializedAndThrowException()
      handleRequest(x, sender(), {

        // Handle requests
        case x: Read =>
          withContextActorRef(x.fromContext,
            (contextActorRef) => {
              read(contextActorRef, x.key,
                (data) => respondTo(x, ReadResponse(data, x.fromContext)), // send response with data
                (error) => throwExFromMessage(x, "Error while reading from context " + x.fromContext + ". " + error))
            },
            (error) => throwExFromMessage(x, "Error while getting the context actor ref. Context:" + x.fromContext + ". Error: " + error))

        case x: Write =>
          withContextActorRef(x.toContext,
            (contextActorRef) => {
              write(contextActorRef, x.key,
                () => x.value, // data to write
                () => respondTo(x, new WriteResponse), // success
                (error) => throwExFromMessage(x, "Error while writing to context " + x.toContext + ". " + error))
            },
            (error) => throwExFromMessage(x, "Error while getting the context actor ref. Context:" + x.toContext + ". Error: " + error))
      })
  }

  def handleContextSpawned(key:String, actorRef:ActorRef) {
    _managedContexts.getOrElse(key, () => _managedContexts.put(key, actorRef))
  }

  /**
   * Ensures that the contextActorRef-continuation gets a ActorRef to a running
   * context actor else calls the error continuation.
   * @param contextKey The key of the context
   * @param contextActorRef The success continuation with the ref to the running actor
   * @param error The error continuation.
   */
  def withContextActorRef(contextKey:String,
                          contextActorRef:(ActorRef) => Unit,
                          error:(Exception) => Unit) {
    contextExists(contextKey,
      () => {
        contextRunning(contextKey,
          (actorRef) => contextActorRef(actorRef),          // context is already running
          () => {
            startContext(contextKey,                        // try to start context
              (actorRef) => {
                handleContextSpawned(contextKey, actorRef)
                contextActorRef(actorRef)                   // context was started and is running
              },
              (exception) => error(exception)               // context could not be started
            )
          })
      },
      () => error(new Exception("The context with key " + contextKey + " does not exist."))
    )
  }

  /**
   * Looks first if the context is already managed by this actor, then asks
   * the supervising ContextGroupOwner.
   * @param contextKey The key of the context
   * @param yes yes-continuation
   * @param no no-continuation
   */
  def contextExists(contextKey:String,
                    yes:() => Unit,
                    no:() => Unit)  {
    if (_managedContexts.contains(contextKey)) {
      yes()
    } else onResponseOf(
      ContextExists(contextKey), _contextGroupOwner, {
        case ContextExistsResponse(true) => yes()
        case ContextExistsResponse(false) => no()
        case x:UnexpectedErrorResponse => throwExFromMessage(x)
      }
    )
  }

  /**
   * Checks if the specified context is already managed by this accessor.
   * @param contextKey The key of the context
   * @param yes yes-continuation
   * @param no no-continuation
   */
  def contextRunning(contextKey:String,
                     yes:(ActorRef) => Unit,
                     no:() => Unit) {
    if (_managedContexts.contains(contextKey))
      yes(_managedContexts.get(contextKey).get)
    else
      no()
  }

  /**
   * Asks the ContextGroupOwner to spawn the specified context.
   * @param contextKey The key of the context
   * @param started continuation to execute when the context was started
   * @param error continuation to execute when the starting failed
   */
  def startContext(contextKey:String,
                   started:(ActorRef) => Unit,
                   error:(Exception) => Unit) {
    onResponseOf(
      SpawnContext(contextKey), _contextGroupOwner, {
        case x:SpawnContextResponse => started(x.actorRef)
        case x:UnexpectedErrorResponse => error(new Exception("Error while starting the context " + contextKey + ". See context owners log."))
      }
    )
  }

  def read(actorRef:ActorRef, 
           dataKey:String,
           data:(String) => Unit,
           error:(String) => Unit) {

    data("Bla")
  }

  def write(actorRef:ActorRef,
             dataKey:String,
             data:() => String,
             success:() => Unit,
             error:(String) => Unit) {
    //val toWriteString = data
    success()
  }



  def maySendUninitializedAndThrowException() {
    if (_contextGroupOwner == null && _profile == null) {
      sender ! UninitializedResponse(List("PropagateContextOwner","PropagateProfile"))
      throwUninitializedException()
    } else if (_contextGroupOwner == null) {
      sender ! UninitializedResponse(List("PropagateContextOwner"))
      throwUninitializedException()
    } else if (_profile == null) {
      sender ! UninitializedResponse(List("PropagateProfile"))
      throwUninitializedException()
    }
  }

  def throwUninitializedException () {
    throw new Exception("The context router is not properly initialized. It requires at least a PropagateContextOwner and PropagateProfile event.")
  }
}