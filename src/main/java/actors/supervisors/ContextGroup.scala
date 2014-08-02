package actors.supervisors

import actors.behaviors._
import akka.actor.{Props, ActorRef}
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
class ContextGroup extends WorkerActor {
  /**
   * Manages the access to all context resources and can spawn new contexts by asking the context owner to do so.
   */
  private val _contextResourceManager = new ResourceManager[String, ActorRef](startContext)


  def handleRequest = {
    case x: Read => handle[Read](sender(), x, read)
    case x: Write => handle[Write](sender(), x, write)
  }

  def start(sender: ActorRef, message: Start, started:() => Unit) {
    started() //@todo: Do actual startup
  }

  def stop(sender:ActorRef, message:Stop, stopped:() => Unit) {
    stopped() //@todo: Do the actual stopping
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
                           started: (ActorRef) => Unit,
                           error: (Exception) => Unit) {
    request[SpawnResponse](Spawn(Props[Context], contextKey), self,
      (response) => started(response.actorRef),
      (exception) => error(exception))
  }

  /**
   * Issues a ReadFromContext request to the corresponding context actor.
   */
  private def readFromContext(actorRef: ActorRef,
                              dataKey: String,
                              data: (String) => Unit,
                              error: (Exception) => Unit) {
    request[ReadResponse](ReadFromContext(dataKey), self,
      (response) => data(response.data),
      (exception) => error(new Exception("Error while reading from context. Data key: " + dataKey, exception)))
  }

  /**
   * Issues a WriteToContext request to the corresponding context actor.
   */
  private def writeToContext(actorRef: ActorRef,
                             dataKey: String,
                             data: () => String,
                             success: () => Unit,
                             error: (Exception) => Unit) {
    request[WriteResponse](WriteToContext(dataKey, data()), self,
      (response) => success(),
      (exception) => error(new Exception("Error while writing to context. Data key: " + dataKey, exception)))
  }
}