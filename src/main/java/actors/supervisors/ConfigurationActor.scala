package actors.supervisors

import actors.behaviors.WorkerActor
import akka.actor.ActorRef
import requests.{WriteConfig, Start, ReadConfig, Stop}
import requests.config.{GetContextDataFilePathResponse, GetContextDataFilePath}

class ConfigurationActor extends WorkerActor {

  private val _dataFolder = System.getProperty("home")

  // @todo: make the configuration just another context
  def handleRequest = {
      case x:GetContextDataFilePath => handleGetContextDataFilePath(sender(), x)
      case x:ReadConfig => handleReadConfig(sender(), x)
      case x:WriteConfig => handleWriteConfig(sender(), x)
  }

  def start(sender:ActorRef, message:Start) {
  }

  def stop(sender:ActorRef, message:Stop) {
  }

  private def handleReadConfig(sender:ActorRef, message:ReadConfig) {
  }

  private def handleWriteConfig(sender:ActorRef, message:WriteConfig) {
  }

  private def handleGetContextDataFilePath(sender:ActorRef, message:GetContextDataFilePath) {
    sender ! GetContextDataFilePathResponse(message, _dataFolder)
  }
}