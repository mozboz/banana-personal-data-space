package actors.supervisors

import akka.actor.Actor


class ContextActor extends Actor with RequestResponseActor
                                 with MessageHandler {
  def receive = {
    case _ =>
  }
}