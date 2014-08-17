package actors.supervisors

import actors.behaviors.RequestHandler
import akka.actor.{Actor, ActorRef}
import requests._

class Configuration extends Actor with RequestHandler {

  def receive = handleRequest

  var _profileActor : ActorRef = _

  def handleRequest = {
    case x:ReadConfig => handle[ReadConfig](sender(), x, handleReadConfig)
    case x:WriteConfig => handle[WriteConfig](sender(), x, handleWriteConfig)
    // case x:ReadActorConfig => handle[ReadActorConfig](sender(), x, handleReadActorConfig)
    case x:WriteActorConfig => handle[WriteActorConfig](sender(), x, handleWriteActorConfig)
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
      case "profileActor" =>
        _profileActor = message.value.asInstanceOf[ActorRef]
        sender ! WriteConfigResponse(message)
    }
  }

  /*
  private def handleReadActorConfig(sender:ActorRef, message:ReadActorConfig) {
    message.key match {
      case "file" =>
      case _ => throw new Exception("Unknown config key: " + message.key)
    }
  }
*/

  private def handleWriteActorConfig(sender:ActorRef, message:WriteActorConfig) {
    message.key match {
      case "file" =>
      case _ => throw new Exception("Unknown config key: " + message.key)
    }
  }
}