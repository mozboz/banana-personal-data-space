package actors.supervisors

import actors.behaviors._
import akka.actor.{Props, ActorRef}
import requests._
import utils.BufferedResource


class Profile extends SupervisorActor with Proxy {

  var _localContexts  = new BufferedResource[String, ActorRef]("localContexts")
  var _remoteContexts  = new BufferedResource[String, ActorRef]("remoteContexts")

  def handleRequest = handleResponse orElse {
    case x:Read => handle[Read](sender(), x, read)
    case x:Write => handle[Write](sender(), x, write)
    case x:ContextExists => handle[ContextExists](sender(), x, contextExists)
  }


  def read(sender:ActorRef, message:Read) {
    // @todo: Forward to the corresponding ContextGroup
    _localContexts.withResource((group) => proxy(message, group, sender), (ex) => throw ex)
  }

  def write(sender:ActorRef, message:Write) {
    // @todo: Forward to the corresponding ContextGroup
    _localContexts.withResource((group) => proxy(message, group, sender), (ex) => throw ex)
  }

  def start(sender:ActorRef, message:Start, started:() => Unit) {

    val join = joinN(2, started)

    request[SpawnResponse](Spawn(Props[ContextGroup], "localContexts"), self,
      (response) => {
        _localContexts.set((key, resource, error) => {
          resource(response.actorRef)
          join()
        })
      },
      (ex) => throw ex)

    request[SpawnResponse](Spawn(Props[ContextGroup], "remoteContexts"), self,
      (response) => {
        _remoteContexts.set((key, resource, error) => {
          resource(response.actorRef)
          join()
        })
      },
      (ex) => throw ex)
  }

  def stop(sender:ActorRef, message:Stop, stopped:() => Unit) {

    val join = joinN(2, stopped)

    _localContexts.withResource((actor) => {
      request[KillResponse](Kill("localContexts"), self, (r) => join(), (ex) => throw ex)
    }, (error) => {
      throw error
    })
    _remoteContexts.withResource((actor) => {
      request[KillResponse](Kill("remoteContexts"), self, (r) => join(), (ex) => throw ex)
    }, (error) => {
      throw error
    })
  }

  private def contextExists(sender:ActorRef, message:ContextExists) {
    sender ! ContextExistsResponse(message, exists = true)
  }
}