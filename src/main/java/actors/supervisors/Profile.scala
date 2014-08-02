package actors.supervisors

import actors.behaviors._
import akka.actor.{Props, ActorRef, Actor}
import requests._
import utils.BufferedResource


class Profile extends SupervisorActor {

  var _localContexts  = new BufferedResource[String, ActorRef]("localContexts")
  var _remoteContexts  = new BufferedResource[String, ActorRef]("remoteContexts")

  def handleRequest = handleResponse orElse {
    case x:ContextExists => handleContextExists(sender(), x)
  }

  def start(sender:ActorRef, message:Start, started:() => Unit) {

    val join = joiner(2, started)

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

    val join = joiner(2, stopped)

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

  private def handleContextExists(sender:ActorRef, message:ContextExists) {
    sender ! ContextExistsResponse(message, exists = true)
  }
}