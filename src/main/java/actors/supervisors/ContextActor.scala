package actors.supervisors

import actors.behaviors._
import actors.workers.FilesystemContextActor
import akka.actor.{Props, ActorRef}
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
class ContextActor extends WorkerActor with Proxy  {

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
  /*private val _fsBackendActor = context.actorOf(Props[FilesystemContextActor], context.self.path.name)
  self ! ConnectContextBackend(_fsBackendActor)*/

  def handleRequest = {
    case x: AggregateContext => handle[AggregateContext](sender(), x, aggregateContext)
    case x: ReadFromContext => handle[ReadFromContext](sender(), x, readFromContext)
    case x: WriteToContext => handle[WriteToContext](sender(), x, writeToContext)
    case x: AddReferencedBy => handle[AddReferencedBy](sender(), x, addReferencedBy)
    case x: AddReferenceTo => handle[AddReferenceTo](sender(), x, addReferenceTo)
  }

  def start(sender:ActorRef, message:Start, started:() => Unit) {
    // @todo: Implement setup logic
    // @todo: Which URI does this context have?
    /*_contextBackend.withResource(
      (actor) => {
        actor ! message // @todo: integrate the backend-actor into the initialization-hierarchy by adding it as a child
        sender ! StartupResponse(message)
      },
      (ex) => sender ! ErrorResponse(message, ex)
    )*/
  }

  def stop(sender:ActorRef, message:Stop, stopped:() => Unit) {
    /*_contextBackend.withResource(
      (actor) => {
        actor ! message // @todo: integrate the backend-actor into the initialization-hierarchy by adding it as a child
        aggregateSome(message, List(actor), (response,sender,done) => {
          self ! DisconnectContextBackend
          done()
        })
      },
      (ex) => throw ex)*/
  }

  private def addReferenceTo(sender:ActorRef, message:AddReferenceTo) {
    // @todo: implement the AddReferenceTo behavior
    _referencedContexts.put(message.uri, message.actor)
  }

  private def addReferencedBy(sender:ActorRef, message:AddReferencedBy) {
    // @todo: implement the AddReferencedBy behavior
    _referencedByContexts.put(message.uri, message.actor)
  }

  private def aggregateContext(sender:ActorRef, message:AggregateContext) {
    // @todo: implement the AggregateContext behavior
    _aggregatesContexts.put(message.uri, message.actor)
  }

  private def readFromContext(sender:ActorRef, message:ReadFromContext) {
    withContextBackend(
      (backend) => proxy(message, backend, sender),
      (exception) => throw exception)
  }

  private def writeToContext(sender:ActorRef, message:WriteToContext) {
    withContextBackend(
      (backend) => proxy(message, backend, sender),
      (exception) => throw exception)
  }

  private def withContextBackend (withContextBackend : (ActorRef) => Unit,
                                  onError : (Exception) => Unit) {
    if (!_contextBackend.isInitialized) {
      // @todo: Notify "someone" about the missing dependency (maybe throttled)
    }
    _contextBackend.withResource(withContextBackend, onError)
  }
}