package actors.supervisors

import actors.behaviors._
import akka.actor.Actor
import requests.{Shutdown, ContextExistsResponse, ContextExists}


class ProfileActor extends Actor with Requester
                                 //with RequestResponder
                                 with MessageHandler {

  def receive = handleResponse orElse {

    case x:Shutdown => // @todo: implement!

    //case x:Response => handleResponse(x)
    //case x:Request => handleRequest(x,sender(),{
      case x:ContextExists =>
        sender ! ContextExistsResponse(x, exists = true)
    //})
  }
}
