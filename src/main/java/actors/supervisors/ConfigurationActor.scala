package actors.supervisors

import actors.behaviors.RequestHandler
import akka.actor.{Actor, ActorRef}
import requests.{ReadConfigResponse, WriteConfig, ReadConfig}

class ConfigurationActor extends Actor with RequestHandler {

  def receive = handleRequest

  def handleRequest = {
      case x:ReadConfig => handle[ReadConfig](sender(), x, handleReadConfig)
      case x:WriteConfig => handle[WriteConfig](sender(), x, handleWriteConfig)
  }

  private def handleReadConfig(sender:ActorRef, message:ReadConfig) {
    if (message.key == "dataFolder")
      sender ! ReadConfigResponse(message, System.getProperty("home"))

    throw new Exception("Unknown config key: " + message.key)
  }

  private def handleWriteConfig(sender:ActorRef, message:WriteConfig) {
    throw new Exception("Writing to the configuration is not yet implemented.")
  }
}