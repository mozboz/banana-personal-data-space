package actors.supervisors

import actors.behaviors._
import actors.workers.FilesystemContextActor
import akka.actor.{Props, Actor, ActorRef}
import akka.event.LoggingReceive
import events.{DisconnectFile, ConnectContextBackend, DisconnectContextBackend}
import requests._
import utils.BufferedResource

import scala.collection.immutable.HashSet

/**
 * Represents a logical context. This can be either local or remote, depending on the connected context-backend.
 *
 * Handles the following events:
 * * ConnectContextBackend
 * * DisconnectContextBackend
 *
 * Proxies the following request to the attached backend
 * * ReadFromContext
 * * WriteToContext
 */
class ContextActor extends Actor with RequestProxy {

  // @todo: Implement aggregation
  // @todo: Implement the metadata stuff
  private val _referencedContexts = new HashSet[String]
  private val _referencedByContexts = new HashSet[String]
  private val _aggregatesContexts = new HashSet[String]

  private val _contextBackend = new BufferedResource[String,ActorRef]("ContextBackend")

  // @todo: only for testing
  private val _fsBackendActor = context.actorOf(Props[FilesystemContextActor], context.self.path.name)
  context.self ! ConnectContextBackend(_fsBackendActor)

  def receive = LoggingReceive({

    case x:ConnectContextBackend => _contextBackend.set((a, loaded, b) => loaded(x.actorRef))
    case x:DisconnectContextBackend => _contextBackend.reset(None)

    case x:Shutdown =>
      _contextBackend.withResource(
        (actor) => {
          actor ! x
          context.self ! DisconnectContextBackend
        },
        (ex) => throw ex)

    case x:Response => handleResponse(x)
    case x:Request => handleRequest(x, sender(), {

      case x:AggregateRequest =>

      case x:ReadFromContext =>
        withContextBackend(
          (backend) => proxy(x, backend, context.self),
          (exception) => respond(x, ErrorResponse(x, exception)))

      case x:WriteToContext =>
        withContextBackend(
          (backend) => proxy(x, backend, context.self),
          (exception) => respond(x, ErrorResponse(x, exception)))
    })
  })

  private def withContextBackend (withContextBackend : (ActorRef) => Unit,
                                  onError : (Exception) => Unit) {
    if (!_contextBackend.isInitialized) {
      // @todo: Notify "someone" about the missing dependency (maybe throttled)
    }
    _contextBackend.withResource(withContextBackend, onError)
  }
}