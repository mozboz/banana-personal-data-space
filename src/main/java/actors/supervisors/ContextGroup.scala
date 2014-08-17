package actors.supervisors

import actors.behaviors._
import actors.workers.FilesystemContext
import akka.actor.{Actor, Props, ActorRef}
import requests._

import scala.reflect.ClassTag


class ContextGroup extends SupervisorActor with Proxy with TContextGroup {

  private var _configActorRef : ActorRef = _

  def handleRequest = {
    case x: Read => handle[Read](sender(), x, read)
    case x: Write => handle[Write](sender(), x, write)
  }

  def start(sender: ActorRef, message: Start, started:() => Unit) {
    _configActorRef = message.configRef
    started()
  }

  def stop(sender:ActorRef, message:Stop, stopped:() => Unit) {
    stopped()
  }

  private def read(sender: ActorRef, message: Read) {
    withContext(message.fromContext,
      (context) =>  {
        proxy(message, context, sender)
      },
      (exception) => sender ! ErrorResponse(message, exception)
    )
  }

  private def write(sender: ActorRef, message: Write) {
    withContext(message.toContext,
      (context) => {
        proxy(message, context, sender)
      },
      (exception) => sender ! ErrorResponse(message, exception)
    )
  }

  /**
   * Spawns a Context and starts it. This method is required by the TContextGroup trait.
   * @param contextKey The key of the context
   * @param spawned The spawned-continuation
   * @param error The error-continuation
   */
  def getActor(contextKey: String,
               spawned: (ActorRef) => Unit,
               error: (Exception) => Unit) {
    spawnContext(contextKey, (context) => {
      request[StartResponse](Start(_configActorRef), context, (r) => {
        spawned(context)
      }, error)
    }, error)
  }

  /**
   * Spawns a context and appends a filesystem backend actor
   * as well as metadata actors.
   * @param contextKey
   * @param spawned
   * @param error
   */
  private def spawnContext(contextKey: String,
                           spawned: (ActorRef) => Unit,
                           error: (Exception) => Unit) {
    request[SpawnResponse](Spawn(Props[Context], contextKey), self,
      (response) => {

        val join = joinN(4, () => spawned(response.actorRef))

        spawn[FilesystemContext](response.actorRef, "data",
          (a) => join(),
          (ex) => error(ex))

        spawn[MetadataContextGroup](response.actorRef, "referencedBy",
          (a) => join(),
          (ex) => error(ex))

        spawn[MetadataContextGroup](response.actorRef, "referencesTo",
          (a) => join(),
          (ex) => error(ex))

        spawn[MetadataContextGroup](response.actorRef, "aggregates",
          (a) => join(),
          (ex) => error(ex))
      },
      (ex) => error(ex))
  }

  private def spawn[TToSpawn <: Actor:ClassTag](parent:ActorRef, id:String, spawned:ActorRef => Unit, error:(Exception) => Unit) {
    request[SpawnResponse](Spawn(Props[TToSpawn], id), parent,
      (response) => spawned(response.actorRef),
      (ex) => error(ex))
  }
}