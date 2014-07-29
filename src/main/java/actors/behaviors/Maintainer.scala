package actors.behaviors

import akka.actor.{ActorRef, Actor}
import events.{DisconnectProfile, ConnectProfile}
import requests._
import utils.BufferedResource

import scala.collection.mutable


trait Maintainer extends Actor {

  // @todo: finish this trait

  private var _parentActor:ActorRef = null
  private val _childActors = new mutable.HashSet[ActorRef]

  def profile = new BufferedResource[String, ActorRef]("Profile")
  /**
   * Defines a partial Receive function which can be used by an actor.
   */
  def handleResponse: Receive = new Receive {
    def isDefinedAt(x: Any) = x match {
      case _ => false
    }
    def apply(x: Any) = x match {
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