package actors.supervisors

import actors.behaviors._
import akka.actor.ActorRef
import requests._

/**
 * This actor groups actors which represent the metadata of contexts.
 */
class MetadataContextGroup extends WorkerActor with Proxy with TContextGroup {



  def handleRequest = {
    case x: Read => handle[Read](sender(), x, read)
    case x: Write => handle[Write](sender(), x, write)
  }

  def start(sender: ActorRef, message: Start, started:() => Unit) {
    started()
  }

  def stop(sender:ActorRef, message:Stop, stopped:() => Unit) {
    stopped()
  }

  private def read(sender: ActorRef, message: Read) {
    withContext(message.fromContext,
      (context) => proxy(message, context, sender),
      (exception) => sender ! ErrorResponse(message, exception)
    )
  }

  private def write(sender: ActorRef, message: Write) {
    withContext(message.toContext,
      (context) => proxy(message, context, sender),
      (exception) => sender ! ErrorResponse(message, exception)
    )
  }

  def getActor(contextKey: String,
               spawned: (ActorRef) => Unit,
               error: (Exception) => Unit) {
    // @todo: Find a way to resolve the referenced actor by its key
  }
}