package actors.supervisors

import actors.behaviors._
import akka.actor.{Props, ActorRef, Actor}
import requests._
import utils.BufferedResource


class ProfileActor extends SupervisorActor {

  var _localContexts  = new BufferedResource[String, ActorRef]("localContexts")
  var _remoteContexts  = new BufferedResource[String, ActorRef]("remoteContexts")

  def handleRequest = handleResponse orElse {
    case x:ContextExists => handleContextExists(sender(), x)
  }

  def start(sender:ActorRef, message:Start, started:() => Unit) {

    val joiner = join(2, started)

    aggregateOne(Spawn(Props[ContextGroupAccessorActor], "localContexts"), self, (response,sender) => {
      response match {
        case x:SpawnResponse => _localContexts.set((key, resource, error) => {
          resource.apply(x.actorRef)
          joiner()
        })
        case x:ErrorResponse => throw x.ex
      }
    })
    aggregateOne(Spawn(Props[ContextGroupAccessorActor], "remoteContexts"), self, (response,sender) => {
      response match {
        case x:SpawnResponse => _remoteContexts.set((key, resource, error) => {
          resource.apply(x.actorRef)
          joiner()
        })
        case x:ErrorResponse => throw x.ex
      }
    })
  }

  def stop(sender:ActorRef, message:Stop, stopped:() => Unit) {

    val joiner = join(2, stopped)

    _localContexts.withResource((actor) => {
      // @todo: Doesn't cover the error case yet
      aggregateOne(Kill("localContexts"), self, (response,sender) => joiner())
    }, (error) => {
      throw error
    })
    _remoteContexts.withResource((actor) => {
      // @todo: Doesn't cover the error case yet
      aggregateOne(Kill("remoteContexts"), self, (response,sender) => joiner())
    }, (error) => {
      throw error
    })
  }

  private def handleContextExists(sender:ActorRef, message:ContextExists) {
    sender ! ContextExistsResponse(message, exists = true)
  }
}