package actors.behaviors

import akka.actor.{ActorRef, Actor}
import requests._
import utils.BufferedResource

/**
 * Provides convenient access to the configuration system and handles the startup and shutdown procedure.
 */
trait SystemEvents extends Actor with Requester {
  def config = new BufferedResource[String, ActorRef]("Config")

  // @todo: Add dependency management and discovery

  /**
   * Partial function which handles the configuration system messages.
   */
  def handleSystemEvents: Receive = new Receive {
    def isDefinedAt(x: Any) = x match {
      case x: Startup => true
      case x: Shutdown => true
      case _ => false
    }

    def apply(x: Any) = x match {
      case x: Startup => handleStartupInternal(sender(), x)
      case x: Shutdown => handleShutdown(sender(), x)
    }

    private def handleStartupInternal(sender:ActorRef, message:Startup) {
      config.reset(None)
      config.set((a,loaded,c) => loaded(message.configRef))

      handleStartup(sender, message)
    }

    // @todo: find out why this functions can not be abstract while ContextManager.handleManageContexts compiles fine
    def handleStartup(sender:ActorRef, message:Startup) {
      sender ! SetupResponse(message)
    }

    // @todo: find out why this functions can not be abstract while ContextManager.handleManageContexts compiles fine
    def handleShutdown(sender:ActorRef, message:Shutdown) {
      sender ! ShutdownResponse(message)
    }

    /**
     * Tries to get a value from the config.
     * @param key The config key
     * @param value The value-continuation
     * @param error The error-continuation
     */
    def withConfigValue(key:String, value:Any => Unit, error:Exception => Unit) {
      // @todo: Make the config just a special context
      val request = ReadConfig(key)
      expectResponse(request, (response, sender, handled) => {
        response match {
          case x:ReadConfigResponse =>
            value(x.value)
            handled()
          case ErrorResponse(req, ex) => error(ex)
        }
      })
      config.withResource((actor) => actor ! request, (error) => throw error)
    }

    /**
     * Tries to write a value to the config.
     * @param key The config key
     * @param value The value to write
     * @param success The success-continuation
     * @param error The error-continuation
     */
    def setConfigValue(key:String, value:Any, success:() => Unit, error:Exception => Unit) {
      // @todo: Make the config just a special context
      val request = WriteConfig(key, value)
      expectResponse(request, (response, sender, handled) => {
        response match {
          case x:WriteConfigResponse =>
            success()
            handled()
          case ErrorResponse(req, ex) => error(ex)
        }
      })
      config.withResource((actor) => actor ! request, (error) => throw error)
    }
  }
}