package actors.supervisors

import actors.behaviors._
import akka.actor.Actor
import akka.event.Logging
import requests.{ContextExistsResponse, ContextExists}


class ProfileActor extends Actor with Requester
                                 with RequestResponder
                                 with MessageHandler {
  val log = Logging(context.system, this)
  setLoggingAdapter(log)

  def receive = {
    case x:Response => handleResponse(x)
    case x:Request => handleRequest(x,sender(),{
      case x:ContextExists =>
        respond(x, ContextExistsResponse(exists = true))
    })
  }
}
