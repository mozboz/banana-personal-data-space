package actors.behaviors

import java.util.UUID

import akka.actor.{ActorRef, Actor}
import akka.event.LoggingReceive
import requests._
import utils.BufferedResource
import scala.collection.mutable

/**
 * Provides convenient access to the configuration system and handles the startup and shutdown procedure.
 */
abstract class BaseActor extends Actor
                         with Requester
                         with RequestHandler
                         with Aggregator
                         with Supervisor {

  private val _actorId = UUID.randomUUID()
  def actorId = _actorId

  /**
   * Implements the actors receive-function and routes the incoming messages
   * either to handleSystemEvents, handleResponse or handleRequest.
   * handleRequest can be used for user defined message handling code.
   * @return
   */
  def receive = LoggingReceive(
    handleSupervisorMessages orElse
    handleResponse orElse
    handleRequest
  )

  // @todo: Add dependency management and discovery ->
  // @todo: Create a function which takes a list of Request objects and matches them against ever isDefinedAt to get a list of supported Requests.
}