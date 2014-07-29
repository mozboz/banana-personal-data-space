package actors.behaviors

import akka.actor.{ActorRef, Actor}
import events.{DisconnectProfile, ConnectProfile}
import requests._
import utils.BufferedResource


trait ContextManager extends Actor {

  // @todo: finish this trait

  def profile = new BufferedResource[String, ActorRef]("Profile")
  /**
   * Defines a partial Receive function which can be used by an actor.
   */
  def handleResponse: Receive = new Receive {
    def isDefinedAt(x: Any) = x match {
      case x: ConnectProfile => true
      case x: DisconnectProfile => true
      case x: ManageContexts => true
      case x: ReleaseContexts => true
      case _ => false
    }
    def apply(x: Any) = x match {
      case x: ConnectProfile =>  handleConnectProfile(sender(),x)
      case x: DisconnectProfile => handleDisconnectProfile(sender(), x)
      case x: ManageContexts => handleManageContexts(sender(), x)
      case x: ReleaseContexts => handleReleaseContexts(sender(), x)
      case _ => throw new Exception("This function is not applicable to objects of type: " + x.getClass)
    }
  }

  def handleConnectProfile(sender:ActorRef, message:ConnectProfile) {
    profile.set((a,loaded,c) => loaded(message.profileRef))
  }

  def handleDisconnectProfile(sender:ActorRef, message:DisconnectProfile) {
    profile.reset(None)
  }

  def handleManageContexts(sender:ActorRef, message:ManageContexts)

  def handleReleaseContexts(sender:ActorRef, message:ReleaseContexts)
}
