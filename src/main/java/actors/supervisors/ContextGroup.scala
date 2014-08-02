package actors.supervisors

import actors.behaviors._
import actors.workers.FilesystemContextActor
import akka.actor.{Props, ActorRef}
import requests._
import utils.ResourceManager


class ContextGroup extends SupervisorActor with Proxy {

  private val _contexts = new ResourceManager[String, ActorRef](spawnContext)

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

  private def withContext(contextKey: String,
                          withContext: (ActorRef) => Unit,
                          error: (Exception) => Unit) {
    _contexts
      .get(contextKey)
      .withResource(withContext, error)
  }

  private def spawnContext(contextKey: String,
                           spawned: (ActorRef) => Unit,
                           error: (Exception) => Unit) {
    request[SpawnResponse](Spawn(Props[Context], contextKey), self,
      (response) => {
        spawnFilesystemContextActor(response.actorRef,
          (a) => spawned(response.actorRef),
          (ex) => error(ex))
      },
      (ex) => error(ex))
  }

  private def spawnFilesystemContextActor(parent:ActorRef,
                                          spawned:(ActorRef) => Unit,
                                          error:(Exception) => Unit) {
    request[SpawnResponse](Spawn(Props[FilesystemContextActor], self.path.name), parent,
      (response) => spawned(response.actorRef),
      (ex) => error(ex))
  }
}