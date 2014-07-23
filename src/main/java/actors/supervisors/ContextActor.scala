package actors.supervisors

import actors.behaviors.{Requester, MessageHandler, Request, RequestResponder}
import actors.workers.FilesystemContextActor
import akka.actor.{Props, Actor, ActorRef}
import akka.event.LoggingReceive
import events.{ConnectContextBackend, DisconnectContextBackend}
import requests._
import utils.BufferedResource

import scala.collection.immutable.HashSet


class ContextActor extends Actor with RequestResponder
                                 with Requester
                                 with MessageHandler {

  // @todo: Implement the metadata stuff
  private val _referencedContexts = new HashSet[String]
  private val _referencedByContexts = new HashSet[String]
  private val _aggregatesContexts = new HashSet[String]

  private val _contextBackend = new BufferedResource[String,ActorRef]("ContextBackend")

  // @todo: only for testing
  private val _fsBackendActor = context.actorOf(Props[FilesystemContextActor], context.self.path.name)
  context.self ! ConnectContextBackend(_fsBackendActor)

  def receive = LoggingReceive({

    case x:ConnectContextBackend => _contextBackend.set((a, loaded, b) => loaded(x.actorRef))
    case x:DisconnectContextBackend => _contextBackend.reset(None)

    case x:Request => handleRequest(x, sender(), {

      case x:AggregateRequest =>

      case x:ReadFromContext =>
        withContextBackend(
          (backend) => proxyRead(x, backend),
          (exception) => respond(x, ErrorResponse(exception)))

      case x:WriteToContext =>
        withContextBackend(
          (backend) => proxyWrite(x, backend),
          (exception) => respond(x, ErrorResponse(exception)))
    })
  })

  private def proxyRead(request:ReadFromContext, to:ActorRef) {
    // @todo: Here is a problem with the forwarding of the response from the backend context worker to the accessor
    val proxyRequest = ReadFromContext(request.key)
    onResponseOf(proxyRequest, to, context.self,{
      case x => {
        respond(request, x)
      }
    })
  }

  private def proxyWrite(request:WriteToContext, to:ActorRef) {
    // @todo: Here is a problem with the forwarding of the response from the backend context worker to the accessor
    val proxyRequest = WriteToContext(request.key, request.value)
    onResponseOf(proxyRequest, to, context.self,{
      case x => {
        respond(request, x)
      }
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