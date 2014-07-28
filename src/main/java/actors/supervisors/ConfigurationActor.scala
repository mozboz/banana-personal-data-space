package actors.supervisors

import actors.behaviors.MessageHandler
import akka.actor.{ActorRef, Actor}
import requests.config.{GetContextDataFilePathResponse, GetContextDataFilePath}

class ConfigurationActor extends Actor with MessageHandler {

  System.getProperty("foo")
  private val _dataFolder = "/home/james/dev/banana-personal-data-space/profileSystem/"

  def receive = {
      case x:GetContextDataFilePath => handleGetContextDataFilePath(sender(), x)
  }

  private def handleGetContextDataFilePath(sender:ActorRef, message:GetContextDataFilePath) {
    sender ! GetContextDataFilePathResponse(message, _dataFolder)
  }
}
