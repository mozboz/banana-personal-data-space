package actors.supervisors

import actors.behaviors.Requester
import akka.actor.Actor
import akka.event.LoggingReceive

class ContextMetadataManager extends Actor with Requester { // @todo: add "with SystemEvents"s

  def receive = LoggingReceive(handleResponse orElse {
    case _ =>
  })
}
