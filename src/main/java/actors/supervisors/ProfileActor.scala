package actors.supervisors

import actors.behaviors._
import akka.actor.Actor
import requests.{ContextExistsResponse, ContextExists}


class ProfileActor extends Actor with Requester
                                 with RequestResponder
                                 with MessageHandler {

  def receive = {

    case x:events.Shutdown => // @todo: implement!

    case x:Response => handleResponse(x)
    case x:Request => handleRequest(x,sender(),{
      case x:ContextExists =>
        respond(x, ContextExistsResponse(exists = true))
    })
  }
}
