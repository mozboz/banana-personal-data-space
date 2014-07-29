package actors.supervisors

import actors.behaviors._
import actors.workers.FilesystemContextActor
import akka.actor.{Props, Actor, ActorRef}
import akka.event.LoggingReceive
import events.{DisconnectContextBackend, ConnectContextBackend}
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
class ContextActor extends Actor with Requester with Proxy {

  // @todo: Implement the metadata stuff
  private val _referencedContexts = new HashSet[String]
  private val _referencedByContexts = new HashSet[String]
  private val _aggregatesContexts = new HashSet[String]
  private val _contextBackend = new BufferedResource[String,ActorRef]("ContextBackend")

  // @todo: only for testing
  private val _fsBackendActor = context.actorOf(Props[FilesystemContextActor], context.self.path.name)
  self ! ConnectContextBackend(_fsBackendActor)

  def receive = LoggingReceive(handleResponse orElse {
    case x: Setup => handleSetup(sender(), x)
    case x: ConnectContextBackend => handleConnectContextBackend(sender(), x)
    case x: DisconnectContextBackend => handleDisconnectContextBackend(sender(), x)
    case x: Shutdown => handleShutdown(sender(), x)
    case x: AggregateRequest => handleAggregateRequest(sender(), x)
    case x: ReadFromContext => handleReadFromContext(sender(), x)
    case x: WriteToContext => handleWriteToContext(sender(), x)
  })

  private def handleSetup(sender:ActorRef, message:Setup) {
    // @todo: Implement setup logic
    _contextBackend.withResource(
      (actor) => {
        actor ! message
        sender ! SetupResponse(message)
      },
      (ex) => sender ! ErrorResponse(message, ex)
    )
  }

  private def handleConnectContextBackend(sender:ActorRef, message:ConnectContextBackend) {
    _contextBackend.set((a, loaded, b) => loaded(message.actorRef))
  }

  private def handleDisconnectContextBackend(sender:ActorRef, message:DisconnectContextBackend) {
    _contextBackend.reset(None)
  }

  private def handleShutdown(sender:ActorRef, message:Shutdown) {
    // @todo: Test if this is suitable
    _contextBackend.withResource(
      (actor) => {
        actor ! message
        onResponseOf(message, actor, self, {
          case x: ShutdownResponse =>
            self ! DisconnectContextBackend
            sender ! ShutdownResponse(message)
          case x: ErrorResponse => sender ! ErrorResponse(message, x.ex)
        })
      },
      (ex) => throw ex)
  }

  private def handleAggregateRequest(sender:ActorRef, message:AggregateRequest) {
    // @todo: Build context aggregation
  }

  private def handleReadFromContext(sender:ActorRef, message:ReadFromContext) {
    withContextBackend(
      (backend) => proxy(message, backend, sender),
      (exception) => sender ! ErrorResponse(message, exception))
  }

  private def handleWriteToContext(sender:ActorRef, message:WriteToContext) {
    withContextBackend(
      (backend) => proxy(message, backend, sender),
      (exception) => sender ! ErrorResponse(message, exception))
  }

  private def withContextBackend (withContextBackend : (ActorRef) => Unit,
                                  onError : (Exception) => Unit) {
    if (!_contextBackend.isInitialized) {
      // @todo: Notify "someone" about the missing dependency (maybe throttled)
    }
    _contextBackend.withResource(withContextBackend, onError)
  }
}