package actors.supervisors

import actors.behaviors.{MessageHandler, RequestResponder, Requester, Request,Response}
import akka.actor.{ActorRef, Actor}
import akka.event.Logging
import events.{PropagateContextGroupOwner, ContextStopped}
import requests._
import scala.collection.mutable
import requests.{ReadResponse,WriteResponse,ReadFromContext}

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
class ContextGroupAccessorActor extends Actor with Requester
                                              with RequestResponder
                                              with MessageHandler {
  val log = Logging(context.system, this)
  setLoggingAdapter(log)

  val _managedContexts = new mutable.HashMap[String,ActorRef]

  val _startingContexts = new mutable.HashMap[String,mutable.Queue[(ActorRef) => Unit]]()

  var _contextGroupOwner : ActorRef = null

  def receive = {

    // Handle system events
    case x:ContextStopped => _managedContexts.remove(x.key)

    case x:PropagateContextGroupOwner =>
      _contextGroupOwner = x.contextGroupOwnerRef

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
          log.debug("Read " + x.key + " from: " + x.fromContext)
          withContextActorRef(
            contextKey = x.fromContext,
            contextActorRef = (contextActorRef) => {
              read(
                actorRef = contextActorRef,
                dataKey = x.key,
                data = (data) => respond(x, ReadResponse(data, x.fromContext)), // send response with data
                error = (error) => throwExFromMessage(x, "Error while reading from context " + x.fromContext + ". " + error))
            },
            error = (error) => throwExFromMessage(x, "Error while getting the context actor ref. Context:" + x.fromContext + ". Error: " + error))

        case x: Write =>
          withContextActorRef(x.toContext,
            (contextActorRef) => {
              write(
                actorRef = contextActorRef,
                dataKey = x.key,
                data = () => x.value, // data to write
                success = () => respond(x, new WriteResponse),
                error = (error) => throwExFromMessage(x, "Error while writing to context " + x.toContext + ". " + error))
            },
            (error) => throwExFromMessage(x, "Error while getting the context actor ref. Context:" + x.toContext + ". Error: " + error))
      })
  }

  /**
   * Calls collect on each call and stores its return value in the
   * _startingContexts collection. Calls execute on first collected
   * item for key.
   * @param key The key
   * @param collect The function which returns the data to collect
   * @param start The function which should be called on the first collection of a key
   * @param process Must be called when the execution is finished from within 'start'
   * @todo: Make generic and wrap hashset away
   */
  def collectStartProcess(key:String,
                            collect:(ActorRef) => Unit,
                            start:((ActorRef) => Unit) => Unit,
                            process : (ActorRef) => Unit) {
    if (_startingContexts.contains(key)){
      _startingContexts.get(key).get.enqueue(collect)
    } else {
      val queue = mutable.Queue[(ActorRef) => Unit](collect)
      _startingContexts.put(key, queue)
      start(process)
    }
  }

  def handleContextSpawned(key:String, actorRef:ActorRef) {
    if (!_managedContexts.contains(key))
      _managedContexts.put(key, actorRef)
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
    contextRunning(
      contextKey = contextKey,
      yes = (actorRef) => {
        contextActorRef(actorRef) // context is already running
      },
      no = () => contextExists(
        contextKey = contextKey,
        yes = () => {

          // It happens, that a lot of requests for the same context
          // arrive while this context is still starting.
          // So this function aims to collect all the requests
          // in a queue and execute them when the actor is ready.
          collectStartProcess(contextKey,
            collect = (actorRef) => {
              contextActorRef(actorRef) // collectStartProcess was called with a key and we want to execute
                                        // contextActorRef(actorRef:ActorRef) when the actor was started
            },
            start = (process) => {
              startContext(contextKey, // this is called on the first call to collectStartProcess with a distinct key.
                                       // It starts the context and calls "process" when finished
                (actorRef) => {
                  process.apply(actorRef) // process all queued requests using "collectStartProcess's" process-continuation
                },
                (exception) => {
                  error(exception) // context could not be started
                })
            },

            process = (actorRef) => {
              handleContextSpawned(contextKey, actorRef) // make context available

              while (_startingContexts.get(contextKey).get.size > 0) { // process all pending requests
                val queue = _startingContexts.get(contextKey).get
                val everyContinuation = queue.dequeue()
                everyContinuation.apply(actorRef)
            }})
        },
        no = () => error(new Exception("The context with key " + contextKey + " does not exist."))
      )
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
      ContextExists(contextKey), _contextGroupOwner, context.self, {
        case ContextExistsResponse(true) =>
          yes()
        case ContextExistsResponse(false) =>
          no()
        case x:UnexpectedErrorResponse =>
          throwExFromMessage(x)
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
    log.debug("ContextGroupAccessor.startContext(contextKey:" + contextKey + ")")
    onResponseOf(
      SpawnContext(contextKey), _contextGroupOwner, context.self, {
        case x:SpawnContextResponse => started(x.actorRef)
        case x:UnexpectedErrorResponse => error(new Exception("Error while starting the context " + contextKey + ". See context owners log."))
      }
    )
  }

  var counter = 0

  def read(actorRef:ActorRef,
           dataKey:String,
           data:(String) => Unit,
           error:(String) => Unit) {

    onResponseOf(ReadFromContext(dataKey), actorRef, context.self, {
      case x:ReadResponse =>
        data(x.data)
        counter = counter + 1
    })
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
    if (_contextGroupOwner == null) {
      // @todo:Notify sender about the exact operation that failed because of the uninitialized state
      sender ! UninitializedResponse(List("PropagateContextOwner"))
      throwUninitializedException()
    }
  }

  def throwUninitializedException () {
    throw new Exception("The context router is not properly initialized. It requires at least a PropagateContextOwner and PropagateProfile event.")
  }
}