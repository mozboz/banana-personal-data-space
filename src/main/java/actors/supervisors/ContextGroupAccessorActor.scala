package actors.supervisors

import actors.behaviors._
import akka.actor.ActorRef
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
class ContextGroupAccessorActor extends WorkerActor {

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


  def handleRequest = {
    case x: Read => handle[Read](sender(), x, read)
    case x: Write => handle[Write](sender(), x, write)
  }

  def start(sender: ActorRef, message: Start) {

  }

  def stop(sender:ActorRef, message:Stop) {
    _contextResourceManager.keys().foreach(
      (key) => _contextResourceManager
        .get(key)
        .withResource(
          (res) => res ! message,
          (ex) => throw ex))
  }

  private def read(sender: ActorRef, message: Read) {
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

  private def write(sender: ActorRef, message: Write) {
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

    aggregateOne(SpawnContext(contextKey), contextGroupOwner, (response, sender, done) => {
      response match {
        case x: SpawnContextResponse => started(x.actorRef)
        case x: ErrorResponse => error(new Exception("Error while starting the context " + contextKey, x.ex))
      }
    })
  }

  /**
   * Issues a ReadFromContext request to the corresponding context actor.
   */
  private def readFromContext(actorRef: ActorRef,
                              dataKey: String,
                              data: (String) => Unit,
                              error: (Exception) => Unit) {
    aggregateOne(ReadFromContext(dataKey), actorRef, (response, sender, done) => {
      response match {
        case x: ReadResponse => data(x.data)
        case x: ErrorResponse => error(new Exception("Error while reading from context. Data key: " + dataKey, x.ex))
      }
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
    aggregateOne(WriteToContext(dataKey, data()), actorRef, (response, sender, done) => {
      response match {
        case x: WriteResponse => success()
        case x: ErrorResponse => error(new Exception("Error while writing to context. Data key: " + dataKey, x.ex))
      }
    })
  }
}