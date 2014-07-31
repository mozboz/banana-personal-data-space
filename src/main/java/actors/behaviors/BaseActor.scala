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
                         with Aggregator
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
    handleSystemEvents orElse
    handleResponse orElse
    handleRequest
  )

  /**
   * Replaces the default actor receive function.
   */
  def handleRequest: Receive



  // @todo: Add dependency management and discovery ->
  // @todo: Create a function which takes a list of Request objects and matches them against ever isDefinedAt to get a list of supported Requests.

  /**
   * Partial function which handles the configuration system messages.
   */
  def handleSystemEvents: Receive = new Receive {
    def isDefinedAt(x: Any) = x match {
      case x: Startup => true
      case x: Shutdown => true
      case x: AddConfigurable => true
      case x: RemoveConfigurable => true
      case _ => false
    }

    /*
     * Order for the requests should be as follows:
     * 1. AddChildren
     * 2. Startup (recursive)
     * 3. Shutdown (recursive)
     * 4. RemoveChildren
     */
    def apply(x: Any) = x match {
      case x: Startup => handleStartupInternal(sender(), x)
      case x: Shutdown => handleShutdownInternal(sender(), x)
      case x: AddConfigurable => handleAddConfigurable(sender(), x)
      case x: RemoveConfigurable => handleRemoveConfigurable(sender(), x)
      case _ => throw new Exception("This function can not be applied to a value of " + x.getClass)
    }
  }
}