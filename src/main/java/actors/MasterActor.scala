package actors

import akka.actor.Actor

class MasterActor extends Actor {

  case class Add(item : Addable, reference : ProfileReference)

  sealed abstract class Addable(key: String)
  sealed abstract class ProfileReference (val uri : String)

  case class Context(key: String) extends Addable(key)
  case class Content(key: String, value: String) extends Addable(key)

  case class ContextReference(profile: ProfileReference, key : String) extends ProfileReference(profile.uri)

  def receive = {
    case Add(item: Context, to: ProfileReference) => sender ! "omg hai"
    case Add(item: Content, to: ProfileReference) => sender ! "omg hai"
  }
}