package actors.supervisors

import actors.behaviors.{Request, MessageHandler}
import akka.actor.Actor
import requests.config.{GetContextDataFilePathResponse, GetContextDataFilePath}

class ConfigurationActor extends Actor with MessageHandler {

  private val _dataFolder = "/home/daniel/profileSystem/"

  def receive = {
    //case x:Request => handleRequest(x, context.self, {
      case x:GetContextDataFilePath =>
        sender ! GetContextDataFilePathResponse(x, _dataFolder)
    //})
  }
}
