package actors.workers

import actors.behaviors.{MessageHandler, Request, RequestResponder}
import akka.actor.Actor
import akka.event.Logging
import requests.{Read, ReadResponse}


class TestResponder extends Actor
                    with RequestResponder
                    with MessageHandler  {

  val log = Logging(context.system, this)

  setLoggingAdapter(log)

  var counter = 0

  def receive = {
    case x:Request =>
      handleRequest(x, sender(), {
        case x: Read =>
          counter = counter + 1
          log.debug("Sending response:" + counter)
          respond(x, ReadResponse(counter.toString, x.fromContext))
      })
  }
}