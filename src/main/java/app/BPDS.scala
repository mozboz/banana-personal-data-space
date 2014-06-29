package app

import akka.actor.{Actor, ActorSystem, Props}

trait Action
trait Message
trait Thing

trait UrlAdressable {
  def getUrl = ""
}

trait Identifyable {
  def getId = ""
}

trait Sendable extends UrlAdressable

trait Store

case class Profile(url : String)
  extends Store
  with    UrlAdressable {
  override def getUrl = url
}

case class Context(id : String)
  extends Thing
  with    Store
  with    Identifyable {
  override def getId = id
}

case class Item(id : String)
  extends Thing
  with    Identifyable{
  override def getId = id
}

case class Add   (val item : Thing with Identifyable, val to   : Store)  extends Action
// case class Added (val item : Thing with Identifyable, val to   : Store)  extends Message

case class Update(val item : Thing with Identifyable, val in   : Store)  extends Action
case class Remove(val item : Thing with Identifyable, val from : Store)  extends Action


class EndpointActor extends Actor {
  def receive = {
    case Add(item:Context,to:Profile) => throw new Exception("Let it crash: " + item.getId + "->" + to.getUrl);
    case Add(item:Item,to:Context) => throw new Exception("Let it crash: " + item.getId + "->" + to.getId);
    // case Added(item:Item, to:Context) => throw new Exception("Remote profile added: " + item.getId + "->" + to.getId);
  }
}

object BPDS extends App {

  implicit val system = ActorSystem("ProfileSystem")
  val endpoint = system.actorOf(Props[EndpointActor], name = "EndpointActor")  // the local actor

  endpoint !  Add(item=Context("context name"), to=Profile("http://profile.daniel.de"))
  endpoint !  Add(item=Item("item name"), to=Context("context name"))
}