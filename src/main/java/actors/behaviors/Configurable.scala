package actors.behaviors

import akka.actor.{Actor, ActorRef}
import requests.{WriteConfig, ReadConfigResponse, ReadConfig}
import utils.BufferedResource

/**
 * Makes an actor configurable.
 */
trait Configurable extends Actor with Aggregator {

  private val _configActor = new BufferedResource[String, ActorRef]("Config")

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
