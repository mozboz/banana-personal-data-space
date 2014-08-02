package actors.supervisors

import actors.behaviors.RequestHandler
import akka.actor.{Actor, ActorRef}
import requests.{ReadConfigResponse, WriteConfig, ReadConfig}

class Configuration extends Actor with RequestHandler {

  def receive = handleRequest

  var _profileActor : ActorRef = _

  def handleRequest = {
      case x:ReadConfig => handle[ReadConfig](sender(), x, handleReadConfig)
      case x:WriteConfig => handle[WriteConfig](sender(), x, handleWriteConfig)
  }

  private def handleReadConfig(sender:ActorRef, message:ReadConfig) {
    message.key match {
      case "dataFolder" => sender ! ReadConfigResponse(message, System.getProperty("home"))
      case "profileActor" => sender ! ReadConfigResponse(message, _profileActor)
      case _ => throw new Exception("Unknown config key: " + message.key)
    }
  }

  private def handleWriteConfig(sender:ActorRef, message:WriteConfig) {
    message.key match {
      case "profileActor" => _profileActor = message.value.asInstanceOf
    }
  }
}