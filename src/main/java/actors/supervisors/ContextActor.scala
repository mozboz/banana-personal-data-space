package actors.supervisors

import actors.behaviors.{Requester, MessageHandler, Request, RequestResponder}
import actors.workers.LocalContextActor
import akka.actor.{Props, Actor, ActorRef}
import akka.event.LoggingReceive
import events.{ConnectContextBackend, DisconnectContextBackend}
import requests._
import utils.BufferedResource

import scala.collection.immutable.HashSet


class ContextActor extends Actor with RequestResponder
                                 with Requester
                                 with MessageHandler {

  private val _referencedContexts = new HashSet[String]
  private val _referencedByContexts = new HashSet[String]
  private val _aggregatesContexts = new HashSet[String]

  private val _contextBackend = new BufferedResource[String,ActorRef]("ContextBackend")

  // @todo: only for testing
  private val _fsBackendActor = context.actorOf(Props[LocalContextActor], context.self.path.name)
  context.self ! ConnectContextBackend(_fsBackendActor)

  def receive = LoggingReceive({

    case x:ConnectContextBackend => _contextBackend.set((a, loaded, b) => loaded(x.actorRef))
    case x:DisconnectContextBackend => _contextBackend.reset(None)

    case x:Request => handleRequest(x, sender(), {

      case x:AggregateRequest =>

      case x:ReadFromContext =>
        withContextBackend(
          (backend) => proxyRequest(x, backend),
          (exception) => respond(x, ErrorResponse(exception)))

      case x:WriteToContext =>
        withContextBackend(
          (backend) => proxyRequest(x, backend),
          (exception) => respond(x, ErrorResponse(exception)))
    })
  })

  private def proxyRequest(request:Request, to:ActorRef) {
    onResponseOf(request, to, context.self,{
      case x => respond(request, x)
    })
  }

  private def withContextBackend (withContextBackend : (ActorRef) => Unit,
                                  onError : (Exception) => Unit) {
    if (!_contextBackend.isInitialized) {
      // @todo: Notify "someone" about the missing dependency (maybe throttled)
    }
    _contextBackend.withResource(withContextBackend, onError)
  }
}