package actors.workers

import actors.behaviors.{MessageHandler, Request, RequestResponder}
import akka.actor.Actor
import akka.event.LoggingReceive
import requests.{WriteResponse, ReadFromContext, ReadResponse, WriteToContext}


class ContextActor extends Actor with RequestResponder
                                 with MessageHandler {
  def receive = LoggingReceive({
    case x:Request => handleRequest(x, sender(), {
      case x:ReadFromContext => {
        // @todo; implement read from context
        respond(x, ReadResponse("bla", context.self.path.name))
      }
      case x:WriteToContext => {
        // @todo; implement write to context
        respond(x, WriteResponse())
      }
    })
  })
}