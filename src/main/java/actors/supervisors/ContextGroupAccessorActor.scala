package actors.supervisors

import actors.behaviors._
import akka.actor.{ActorRef, Actor}
import akka.event.LoggingReceive
import events.{DisconnectContextGroupOwner, ConnectContextGroupOwner, ContextStopped}
import requests._
import requests.{ReadResponse,ReadFromContext}
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
class ContextGroupAccessorActor extends Actor with Requester
                                              with RequestResponder
                                              with MessageHandler{

  /**
   * Represents a future for the context group owner actor. This actor ref is necessary to
   * spawn contexts so it blocks all following requests if its not available and the requested
   * contexts are not running.
   */
  private val _lazyContextGroupOwner = new BufferedResource[String,ActorRef]("ContextGroupOwner")

  /**
   * Manages the access to all context resources and can spawn new contexts by asking the context owner to do so.
   */
  private val _contextResourceManager = new ResourceManager[String,ActorRef](
    (a,b,c) => {
      _lazyContextGroupOwner.withResource(
        (contextGroupOwner) => startContext(a,contextGroupOwner,b,c),
        (exception) => c.apply(exception)
      )
    })

  def receive = LoggingReceive({

    case x:ContextStopped =>

    case x:ConnectContextGroupOwner =>_lazyContextGroupOwner.set((a, loaded, b) => loaded(x.contextGroupOwnerRef))
    case x:DisconnectContextGroupOwner => _lazyContextGroupOwner.reset(None)

    case x:Response => handleResponse(x)

    case x:Request => handleRequest(x, sender(), {

        case x: Read =>
          withContext(x.fromContext)(
            (contextActorRef) => {
              readFromContext(
                actorRef = contextActorRef,
                dataKey = x.key,
                data = (data)
                  => respond(x, ReadResponse(data, x.key)),
                error = (ex)
                  => respond(x, ErrorResponse(ex))
              )
            },
            onError = (ex)
              => respond(x, ErrorResponse(ex))
        )

        case x: Write =>
          withContext(x.toContext)(
            (contextActorRef) => {
              writeToContext(
                actorRef = contextActorRef,
                dataKey = x.key,
                data = () => x.value,
                success = ()
                  => respond(x, WriteResponse()),
                error = (ex)
                  => respond(x, ErrorResponse(ex))
              )
            },
            onError = (ex)
              => respond(x, ErrorResponse(ex))
        )})
  })


  /**
   * Curried function which takes the contextKey first and can then
   * be used to enqueue actions.
   */
  private def withContext(contextKey:String)
                         (withContext : (ActorRef) => Unit,
                          onError : (Exception) => Unit) {
    _contextResourceManager
      .get(contextKey)
      .withResource(withContext, onError)
  }

  /**
   * Asks the ContextGroupOwner to spawn the specified context.
   */
  private def startContext(contextKey:String,
                           contextGroupOwner : ActorRef,
                           started:(ActorRef) => Unit,
                           error:(Exception) => Unit) {

    if (!_lazyContextGroupOwner.isInitialized) {
      // @todo: Notify "someone" about the missing dependency (maybe throttled)
    }

    onResponseOf(
      SpawnContext(contextKey), contextGroupOwner, context.self,
      {
        case x:SpawnContextResponse => started(x.actorRef)
        case x:ErrorResponse => error(new Exception("Error while starting the context " + contextKey, x.ex))
      })
  }

  /**
   * Issues a ReadFromContext request to the corresponding context actor.
   */
  private def readFromContext(actorRef:ActorRef,
                              dataKey:String,
                              data:(String) => Unit,
                              error:(Exception) => Unit) {
    onResponseOf(
      ReadFromContext(dataKey), actorRef, context.self,
      {
        case x:ReadResponse => data(x.data)
        case x:ErrorResponse => error(new Exception("Error while reading from context. Data key: " + dataKey, x.ex))
      })
  }

  /**
   * Issues a WriteToContext request to the corresponding context actor.
   */
  private def writeToContext(actorRef:ActorRef,
                             dataKey:String,
                             data:() => String,
                             success:() => Unit,
                             error:(Exception) => Unit) {
    onResponseOf(
      WriteToContext(dataKey, data()), actorRef, context.self,
      {
        case x:WriteResponse => success()
        case x:ErrorResponse => error(new Exception("Error while writing to context. Data key: " + dataKey, x.ex))
      })
  }
}