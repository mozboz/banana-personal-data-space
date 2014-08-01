package actors.behaviors

import akka.actor.ActorRef
import akka.event.LoggingReceive
import requests.{Stop, Start}


abstract class SupervisorActor extends WorkerActor with Supervisor {

  override def receive = LoggingReceive(
    handleConfigurableMessages orElse
      handleSupervisorMessages orElse
      handleResponse orElse
      handleRequest
  )
}
