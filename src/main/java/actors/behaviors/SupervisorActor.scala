package actors.behaviors

import akka.event.LoggingReceive

abstract class SupervisorActor extends WorkerActor
                               with Supervisor {

  override def receive = LoggingReceive(
    handleConfigurableMessages orElse
    handleSupervisorMessages orElse
    handleResponse orElse
    handleRequest
  )
}