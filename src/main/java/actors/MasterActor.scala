package actors

import akka.actor.Actor

class MasterActor extends Actor {

  case class Add(item : Addable, reference : ProfileReference)
  case class Get(item : Addable, reference : ProfileReference)

  sealed abstract class Addable(key: String)

  class ProfileReference (val uri : String)

  class ContextReference(val profile: ProfileReference, key : String) extends ProfileReference(profile.uri)

  class ContentReference(val context: ContextReference, key : String) extends ContextReference(context.profile, key)

  case class Context(key: String) extends Addable(key)
  case class Content(key: String, value: String) extends Addable(key)

  def receive = {
    case Add(item: Context, to: ProfileReference) => sender ! "omg hai"
    case Add(item: Content, to: ProfileReference) => sender ! "omg hai"
  }
}