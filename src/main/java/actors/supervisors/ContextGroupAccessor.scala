package actors.supervisors

import java.util.UUID
import akka.actor.{ActorRef, Actor}
import scala.collection.mutable


trait RequestResponse {
  private val _pendingRequests = new mutable.HashMap[UUID,(Response) => (Boolean)]
  private val _pendingResponses = new mutable.HashMap[UUID,(Response) => Unit]

  def handleResponse(x:Response) {
    val processed = _pendingRequests
      .getOrElse(x.requestId, (x:Response) => false)
      .apply(x)

    if (processed)
      _pendingRequests.remove(x.requestId)
  }

  def onResponseOf(message:Message, to:ActorRef, callback:(Response) => (Unit)) {
    // @todo: Add timeout for the case that the response is never provided
    _pendingRequests.put(message.messageId, (x) =>  {
        callback.apply(x)
        true
      })
    to ! message
  }

  def handleRequest(x: Request, sender:ActorRef, handler: (Request) => (Unit)) {
    try {
      // @todo: Add timeout for the case that the response is never provided
      _pendingResponses.put(x.messageId, (response) => {
        sender ! response
      })
      handler.apply(x)
    } catch {
      case e: Exception => {
        val errorResponse = new UnexpectedErrorResponse()
        errorResponse.setRequestId(x.messageId)
        sender ! errorResponse
      }
    }
  }

  def sendResponse(x:Request, y:Response) {
    y.setRequestId(x.messageId)
    _pendingResponses.get(x.messageId).get.apply(y)
    _pendingResponses.remove(x.messageId)
  }
}

trait Message {
  private val _messageId = UUID.randomUUID()
  def messageId = _messageId
}

trait Event extends Message

trait Request extends  Message

trait Response extends Message {
  private var _requestId : UUID = null;

  def setRequestId(messageId:UUID) {
    _requestId = messageId
  }

  def requestId = _requestId
}

/**
 * Reads a value from a context
 * @param key The key
 * @param fromContext The context
 */
case class Read(key:String, fromContext:String) extends Request
case class ReadResponse(data:String, from:String) extends Response

/**
 * Writes a value to a context
 * @param key The key
 * @param value The value
 * @param toContext The context
 */
case class Write(key:String, value:String, toContext:String) extends Request
case class WriteResponse() extends Response

/**
 * Asks if a context exists
 * @param context The name of the context to check
 */
case class ContextExists(context:String) extends Request
case class ContextExistsResponse(exists:Boolean) extends Response

/**
 * Asks if a context could be spawned
 * @param context The name of the context to spawn
 */
case class SpawnContext(context:String) extends Request
case class SpawnContextResponse(actorRef:ActorRef) extends Response

/**
 * Is sent by RequestHandlers to indicate that a request failed
 */
case class UnexpectedErrorResponse() extends Response

/**
 * Notification sent by the context owner to notify everybody involved that
 * a new context was spawned.
 * @param key The key of the spawned context
 * @param actorRef The actor reference
 */
case class ContextSpawned(key:String,actorRef:ActorRef) extends Event
/**
 * Notification sent by the context owner to notify everybody involved that
 * a context was stopped.
 * @param key The key of the stopped context
 */
case class ContextStopped(key:String) extends Event

/**
 * Propagates the context owner to all involved parties
 * @param contextOwnerRef
 */
case class PropagateContextOwner(contextOwnerRef:ActorRef) extends Event

/**
 * Propagates the profile to all involved parties
 * @param profileRef
 */
case class PropagateProfile(profileRef:ActorRef) extends Event

/**
 * Message that contains the names of the event classes
 * which must be sent first in order to use this actor.
 * @param messages A list of case class names
 */
case class Uninitialized(messages:List[String]) extends Message
//@todo: Replace list of strings with a list of types

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
 *   Forwards the following requests:
 *   * Read: forwards every read request which arrives to the context actor and then proxies the reply back to the caller.
 *   * Write:
 */
class ContextGroupAccessor extends Actor with RequestResponse {

  val _managedContexts = new mutable.HashMap[String,ActorRef]

  var _contextOwner : ActorRef = null;
  var _profile : ActorRef = null;

  def receive = {

    // Handle system events
    case x:ContextSpawned => handleContextSpawned(x.key, x.actorRef)
    case x:ContextStopped => _managedContexts.remove(x.key)

    case x:PropagateProfile => _profile = x.profileRef
    case x:PropagateContextOwner => _contextOwner = x.contextOwnerRef

    // Handles all responses to previously issued requests
    case x:Response => {
      maySendUnititializedAndThrowException
      handleResponse(x)
    }

    case x:Request => {
      maySendUnititializedAndThrowException
      handleRequest(x, sender, {

        // Handle requests
        case x: Read => {
          withContextActorRef(x.fromContext,
            (contextActorRef) => {
              read(contextActorRef, x.key,
                (data) => sendResponse(x, ReadResponse(data, x.fromContext)),
                (error) => throwExFromMessage(x, "Error while reading from context " + x.fromContext + ". " + error))
            },
            (error) => throwExFromMessage(x, "Error while getting the context actor ref. Context:" + x.fromContext + ". Error: " + error))
        }

        case x: Write => {
          withContextActorRef(x.toContext,
            (contextActorRef) => {
              write(contextActorRef, x.key,
                () => x.value, // data to write
                () => sendResponse(x, new WriteResponse), // success
                (error) => throwExFromMessage(x, "Error while writing to context " + x.toContext + ". " + error))
            },
            (error) => throwExFromMessage(x, "Error while getting the context actor ref. Context:" + x.toContext + ". Error: " + error))
        }
      })
    }
  }

  def handleContextSpawned(key:String, actorRef:ActorRef) {
    _managedContexts.getOrElse(key, () => _managedContexts.put(key, actorRef))
  }

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
      () => error(new Exception("The context with key " + contextKey + "does not exist."))
    )
  }

  def contextExists(contextKey:String,
                    yes:() => Unit,
                    no:() => Unit)  {
    if (_managedContexts.contains(contextKey)) {
      yes()
    } else {
      onResponseOf(
        ContextExists(contextKey), _profile, {
          case ContextExistsResponse(true) => yes()
          case ContextExistsResponse(false) => no()
          case x:UnexpectedErrorResponse => throwExFromMessage(x)
        }
      )
    }
  }

  def contextRunning(contextKey:String,
                     yes:(ActorRef) => Unit,
                     no:() => Unit) {
    if (_managedContexts.contains(contextKey))
      yes(_managedContexts.get(contextKey).get)
    else
      no()
  }

  def startContext(contextKey:String,
                   started:(ActorRef) => Unit,
                   error:(Exception) => Unit) {
    onResponseOf(
      SpawnContext(contextKey), _contextOwner, {
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
    val toWriteString = data
    success()
  }

  def throwExFromMessage(m:Message, additional:String = "") {
    throw new Exception("Error while processing the message with Id '" + m.messageId + "', Type '" + m.getClass.getName + "': " + additional)
  }

  def maySendUnititializedAndThrowException {
    if (_contextOwner == null && _profile == null) {
      sender ! Uninitialized(List("PropagateContextOwner","PropagateProfile"))
      throwUninitializedExcpetion
    } else if (_contextOwner == null) {
      sender ! Uninitialized(List("PropagateContextOwner"))
      throwUninitializedExcpetion
    } else if (_profile == null) {
      sender ! Uninitialized(List("PropagateProfile"))
      throwUninitializedExcpetion
    }
  }

  def throwUninitializedExcpetion {
    throw new Exception("The context router is not properly initialized. It requires at least a PropagateContextOwner and PropagateProfile event.");
  }
}