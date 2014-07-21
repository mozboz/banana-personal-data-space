package actors.test

import actors.behaviors.{Request, Response, Requester, MessageHandler}
import akka.actor.{ActorRef, Actor}

class TestRequester extends Actor with MessageHandler
                                  with Requester {
  def receive = {
    case x:Response => handleResponse(x)
  }

  def issueRequest(request:Request, to:ActorRef, onResponse : (Response) => Unit) {
    onResponseOf(request, to, context.self, onResponse)
  }
}
