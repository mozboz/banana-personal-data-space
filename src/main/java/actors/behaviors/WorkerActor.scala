package actors.behaviors

import java.util.UUID

import akka.actor.Actor
import akka.event.LoggingReceive

/**
 * Provides convenient access to the configuration system and handles the startup and shutdown procedure.
 */
abstract class WorkerActor extends Actor
                           with Configurable {

  private val _actorId = UUID.randomUUID()
  def actorId = _actorId

  /**
   * Implements the actors receive-function and routes the incoming messages
   * either to handleSystemEvents, handleResponse or handleRequest.
   * handleRequest can be used for user defined message handling code.
   * @return
   */
  def receive = LoggingReceive(
    handleConfigurableMessages orElse
    handleResponse orElse
    handleRequest
  )
}