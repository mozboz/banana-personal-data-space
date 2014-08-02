package actors.supervisors

import actors.behaviors._
import actors.workers.FilesystemContext
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
class Context extends SupervisorActor with Proxy  {

  var _referencedBy  = new BufferedResource[String, ActorRef]("referencedBy")
  var _referencesTo  = new BufferedResource[String, ActorRef]("referencesTo")
  var _aggregates  = new BufferedResource[String, ActorRef]("aggregates")
  var _data  = new BufferedResource[String, ActorRef]("data")

  def handleRequest = {
    case x: Read => handle[Read](sender(), x, read)
    case x: Write => handle[Write](sender(), x, write)
    case x: AggregateContext => handle[AggregateContext](sender(), x, aggregateContext)
    case x: AddReferencedBy => handle[AddReferencedBy](sender(), x, addReferencedBy)
    case x: AddReferenceTo => handle[AddReferenceTo](sender(), x, addReferenceTo)
  }

  def start(sender:ActorRef, message:Start, started:() => Unit) {
    _referencedBy.set((key, ref, error) => {
      ref(context.child(key).get)
    })
    _referencesTo.set((key, ref, error) => {
      ref(getActor(key).get)
    })
    _aggregates.set((key, ref, error) => {
      ref(getActor(key).get)
    })
    _data.set((key, ref, error) => {
      ref(getActor(key).get)
    })

    started()
  }

  def stop(sender:ActorRef, message:Stop, stopped:() => Unit) {
    stopped()
  }

  private def addReferenceTo(sender:ActorRef, message:AddReferenceTo) {
  }

  private def addReferencedBy(sender:ActorRef, message:AddReferencedBy) {
  }

  private def aggregateContext(sender:ActorRef, message:AggregateContext) {
  }

  private def read(sender:ActorRef, message:Read) {
    _data.withResource((data) => proxy(message, data, sender), (ex) => throw ex)
  }

  private def write(sender:ActorRef, message:Write) {
    _data.withResource((data) => proxy(message, data, sender), (ex) => throw ex)
  }
}