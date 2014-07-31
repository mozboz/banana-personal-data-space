package actors.behaviors

import akka.actor.{Actor, ActorRef}
import requests.{WriteConfig, ReadConfigResponse, ReadConfig}
import utils.BufferedResource

/**
 * A configurator provides configuration information.
 */
trait Configurator extends Actor with Aggregator {

  private val _config = new BufferedResource[String, ActorRef]("Config")
  def config = _config

  /**
   * Tries to get a value from the config.
   * @param key The config key
   * @param value The value-continuation
   * @param error The error-continuation
   */
  def readConfig(key:String, value:Any => Unit, error:Exception => Unit) {
    config.withResource(
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
    config.withResource(
      (actor) => aggregateOne(WriteConfig(key, value), actor, (response,sender,done) => {
        success()
      }),
      (exception) => throw exception)
  }
}