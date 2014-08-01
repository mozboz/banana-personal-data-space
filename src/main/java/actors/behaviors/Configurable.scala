package actors.behaviors

import akka.actor.{Actor, ActorRef}
import requests._
import utils.BufferedResource

/**
 * Makes an actor configurable.
 */
trait Configurable extends Actor with Aggregator
                                 with RequestHandler {

  private val _configActor = new BufferedResource[String, ActorRef]("Config")

  def handleConfigurableMessages: Receive = new Receive {
    def isDefinedAt(x: Any) = x match {
      case x: Start => true
      case x: Stop => true
      case _ => false
    }
    def apply(x: Any) = x match {
      case x: Start => handle[Start](sender(), x, handleStart)
      case x: Stop => handle[Stop](sender(), x, handleStop)
      case _ => throw new Exception("This function is not applicable to objects of type: " + x.getClass)
    }
  }

  def handleStart(sender:ActorRef, message:Start) {
    start(sender, message)
    sender ! StartupResponse(message)
  }

  def handleStop(sender:ActorRef, message:Stop) {
    stop(sender, message)
    sender ! StopResponse(message)
  }

  /**
   * When overridden, processes startup logic for the actor.
   */
  def start(sender:ActorRef, message:Start)

  /**
   * When overridden, processes shutdown logic for the actor.
   */
  def stop(sender:ActorRef, message:Stop)

  /**
   * Sets the ref to the config actor which should be consulted.
   * @param configActor
   */
  def setConfig(configActor:ActorRef) {
    _configActor.reset(None)
    _configActor.set((key, actor, ex) => {
      actor.apply(configActor)
    })
  }

  /**
   * Tries to get a value from the config.
   * @param key The config key
   * @param value The value-continuation
   * @param error The error-continuation
   */
  def readConfig(key:String, value:Any => Unit, error:Exception => Unit) {
    _configActor.withResource(
      (actor) => aggregateOne(ReadConfig(key), actor, (response,sender,done) => {
        value(response.asInstanceOf[ReadConfigResponse].value)
      }),
      (exception) => throw exception)
  }

  /**
   * Tries to write a value to the config.
   * @param key The config key
   * @param value The value to write
   * @param success The success-continuation
   * @param error The error-continuation
   */
  def writeConfig(key:String, value:Any, success:() => Unit, error:Exception => Unit) {
    _configActor.withResource(
      (actor) => aggregateOne(WriteConfig(key, value), actor, (response,sender,done) => {
        success()
      }),
      (exception) => throw exception)
  }
}
