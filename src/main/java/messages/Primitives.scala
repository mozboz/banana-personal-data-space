package messages

trait Action
trait Thing

trait UrlAdressable {
  def getUrl = ""
}

trait Identifyable {
  def getId = ""
}

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