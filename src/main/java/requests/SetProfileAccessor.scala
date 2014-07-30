package requests

import akka.actor.ActorRef

case class SetProfileAccessor(profileAccessor:ActorRef)