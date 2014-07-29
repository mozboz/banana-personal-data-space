package actors.supervisors

import actors.behaviors.MessageHandler
import akka.actor.{ActorRef, Actor}
import requests.config.{GetContextDataFilePathResponse, GetContextDataFilePath}

class ConfigurationActor extends Actor with MessageHandler {

  private val _dataFolder = System.getProperty("home")

  // @todo: make the configuration just another context
  def receive = {
      case x:GetContextDataFilePath => handleGetContextDataFilePath(sender(), x)
  }

  private def handleGetContextDataFilePath(sender:ActorRef, message:GetContextDataFilePath) {
    sender ! GetContextDataFilePathResponse(message, _dataFolder)
  }
}