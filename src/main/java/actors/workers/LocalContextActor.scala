package actors.workers

import actors.behaviors.{Request, MessageHandler, RequestResponder}
import akka.actor.Actor
import akka.event.LoggingReceive
import requests.{WriteResponse, WriteToContext, ReadResponse, ReadFromContext}

class LocalContextActor extends Actor with RequestResponder
                                      with MessageHandler {
  def receive = LoggingReceive({
    case x:Request => handleRequest(x, sender(), {
      case x:ReadFromContext => {
        respond(x, ReadResponse("bla", context.self.path.name))
      }
      case x:WriteToContext => {
        respond(x, WriteResponse())
      }
    })
  })
}