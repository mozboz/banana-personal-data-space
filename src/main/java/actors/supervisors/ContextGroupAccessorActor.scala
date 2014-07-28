package actors.supervisors

import actors.behaviors._
import akka.actor.{ActorRef, Actor}
import akka.event.LoggingReceive
import events.{DisconnectContextGroupOwner, ConnectContextGroupOwner, ContextStopped}
import requests._
import requests.{ReadResponse, ReadFromContext}
import utils.{ResourceManager, BufferedResource}

/**
 * Handles all read and write access to context actors which are managed by this group accessor.
 *
 * Handles the following events:
 * * ContextStopped
 * * ConnectContextGroupOwner
 * * DisconnectContextGroupOwner
 *
 * Responds to the following requests:
 * * Read [<- ReadFromContext | <-SpawnContext]
 * * Write [<- WriteToContext | <-SpawnContext]
 *
 * Sends the following requests:
 * * SpawnContext
 * * ReadFromContext
 * * WriteToContext
 */
class ContextGroupAccessorActor extends Actor with Requester {

  /**
   * Represents a future for the context group owner actor. This actor ref is necessary to
   * spawn contexts so it blocks all following requests if its not available and the requested
   * contexts are not running.
   */
  private val _lazyContextGroupOwner = new BufferedResource[String, ActorRef]("ContextGroupOwner")

  /**
   * Manages the access to all context resources and can spawn new contexts by asking the context owner to do so.
   */
  private val _contextResourceManager = new ResourceManager[String, ActorRef](
    (a, b, c) => {
      _lazyContextGroupOwner.withResource(
        (contextGroupOwner) => startContext(a, contextGroupOwner, b, c),
        (exception) => c.apply(exception)
      )
    })


  def receive = LoggingReceive(handleResponse orElse {
    case x: Setup => handleSetup(sender(), x)
    case x: ContextStopped => handleContextStopped(sender(), x)
    case x: ConnectContextGroupOwner => handleConnectContextGroupOwner(sender(),x)
    case x: DisconnectContextGroupOwner => handleDisconnectContextOwner(sender(), x)
    // @todo: Should be a request as confirmation is required
    case x: Shutdown => handleShutdown(sender(), x)

    case x: Read => handleRead(sender(), x)
    case x: Write => handleWrite(sender(), x)
  })

  private def handleSetup(sender: ActorRef, message: Setup) {

  }

  private def handleContextStopped(sender: ActorRef, message:ContextStopped) {

  }

  private def handleRead(sender: ActorRef, message: Read) {
    withContext(message.fromContext)(
      (contextActorRef) => {
        readFromContext(
          actorRef = contextActorRef,
          dataKey = message.key,
          data = (data) => sender ! ReadResponse(message, data),
          error = (ex) => sender ! ErrorResponse(message, ex)
        )
      },
      error = (ex) => sender ! ErrorResponse(message, ex)
    )
  }

  private def handleWrite(sender: ActorRef, message: Write) {
    withContext(message.toContext)(
      (contextActorRef) => {
        writeToContext(
          actorRef = contextActorRef,
          dataKey = message.key,
          data = () => message.value,
          success = () => sender ! WriteResponse(message),
          error = (ex) => sender ! ErrorResponse(message, ex)
        )
      },
      error = (ex) => sender ! ErrorResponse(message, ex)
    )
  }

  private def handleShutdown(sender:ActorRef, message:Shutdown) {
    _contextResourceManager.keys().foreach(
      (key) => _contextResourceManager
        .get(key)
        .withResource(
          (res) => res ! message,
          (ex) => throw ex))
  }

  private def handleDisconnectContextOwner(sender:ActorRef, message:DisconnectContextGroupOwner) {
    _lazyContextGroupOwner.reset(None)
  }

  private def handleConnectContextGroupOwner(sender:ActorRef, message:ConnectContextGroupOwner) {
    _lazyContextGroupOwner.set((a, loaded, b) => loaded(message.contextGroupOwnerRef))
  }

  /**
   * Curried function which takes the contextKey first and can then
   * be used to enqueue actions.
   */
  private def withContext(contextKey: String)
                         (withContext: (ActorRef) => Unit,
                          error: (Exception) => Unit) {
    _contextResourceManager
      .get(contextKey)
      .withResource(withContext, error)
  }

  /**
   * Asks the ContextGroupOwner to spawn the specified context.
   */
  private def startContext(contextKey: String,
                           contextGroupOwner: ActorRef,
                           started: (ActorRef) => Unit,
                           error: (Exception) => Unit) {

    if (!_lazyContextGroupOwner.isInitialized) {
      // @todo: Notify "someone" about the missing dependency (maybe throttled)
    }

    onResponseOf(
    SpawnContext(contextKey), contextGroupOwner, self, {
      case x: SpawnContextResponse => started(x.actorRef)
      case x: ErrorResponse => error(new Exception("Error while starting the context " + contextKey, x.ex))
    })
  }

  /**
   * Issues a ReadFromContext request to the corresponding context actor.
   */
  private def readFromContext(actorRef: ActorRef,
                              dataKey: String,
                              data: (String) => Unit,
                              error: (Exception) => Unit) {
    onResponseOf(
    ReadFromContext(dataKey), actorRef, self, {
      case x: ReadResponse => data(x.data)
      case x: ErrorResponse => error(new Exception("Error while reading from context. Data key: " + dataKey, x.ex))
    })
  }

  /**
   * Issues a WriteToContext request to the corresponding context actor.
   */
  private def writeToContext(actorRef: ActorRef,
                             dataKey: String,
                             data: () => String,
                             success: () => Unit,
                             error: (Exception) => Unit) {
    onResponseOf(
    WriteToContext(dataKey, data()), actorRef, self, {
      case x: WriteResponse => success()
      case x: ErrorResponse => error(new Exception("Error while writing to context. Data key: " + dataKey, x.ex))
    })
  }
}