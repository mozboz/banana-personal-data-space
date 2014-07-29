package actors.supervisors

import actors.behaviors._
import actors.workers.FilesystemContextActor
import akka.actor.{Props, Actor, ActorRef}
import akka.event.LoggingReceive
import events.{DisconnectContextBackend, ConnectContextBackend}
import requests._
import utils.BufferedResource

import scala.collection.mutable

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
class ContextActor extends Actor with Requester with Proxy  { // @todo: add "with SystemEvents"

  // @todo: Implement the metadata stuff
  private val _referencedContexts = new mutable.HashMap[String, ActorRef]
  /**
   * Contains references to the actor which represents the context from which a context is referenced.
   * Key:Uri, Val:ActorRef
   */
  private val _referencedByContexts = new mutable.HashMap[String, ActorRef]
  private val _aggregatesContexts = new mutable.HashMap[String, ActorRef]

  private val _contextBackend = new BufferedResource[String,ActorRef]("ContextBackend")

  // @todo: only for testing
  private val _fsBackendActor = context.actorOf(Props[FilesystemContextActor], context.self.path.name)
  self ! ConnectContextBackend(_fsBackendActor)

  def receive = LoggingReceive(handleResponse orElse {
    case x: Startup => handleSetup(sender(), x)
    case x: ConnectContextBackend => handleConnectContextBackend(sender(), x)
    case x: DisconnectContextBackend => handleDisconnectContextBackend(sender(), x)
    case x: Shutdown => handleShutdown(sender(), x)
    case x: AggregateContext => handleAggregateContext(sender(), x)
    case x: ReadFromContext => handleReadFromContext(sender(), x)
    case x: WriteToContext => handleWriteToContext(sender(), x)
    case x: AddReferencedBy => handleAddReferencedBy(sender(), x)
    case x: AddReferenceTo => handleAddReferenceTo(sender(), x)
  })

  private def handleSetup(sender:ActorRef, message:Startup) {
    // @todo: Implement setup logic
    // @todo: Which URI does this context have?
    _contextBackend.withResource(
      (actor) => {
        actor ! message
        sender ! StartupResponse(message)
      },
      (ex) => sender ! ErrorResponse(message, ex)
    )
  }

  private def handleAddReferenceTo(sender:ActorRef, message:AddReferenceTo) {
    // @todo: implement the AddReferenceTo behavior
    _referencedContexts.put(message.uri, message.actor)
  }

  private def handleAddReferencedBy(sender:ActorRef, message:AddReferencedBy) {
    // @todo: implement the AddReferencedBy behavior
    _referencedByContexts.put(message.uri, message.actor)
  }

  private def handleAggregateContext(sender:ActorRef, message:AggregateContext) {
    // @todo: implement the AggregateContext behavior
    _aggregatesContexts.put(message.uri, message.actor)
  }

  private def handleConnectContextBackend(sender:ActorRef, message:ConnectContextBackend) {
    _contextBackend.set((a, loaded, b) => loaded(message.actorRef))
  }

  private def handleDisconnectContextBackend(sender:ActorRef, message:DisconnectContextBackend) {
    _contextBackend.reset(None)
  }

  private def handleShutdown(sender:ActorRef, message:Shutdown) {
    // @todo: Test if Shutdown behavior is suitable
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