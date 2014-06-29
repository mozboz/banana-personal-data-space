package deleteme

import akka.actor.{Props, ActorSystem}

/*
// Class hierarchy:
trait Expr

class Num(val value : Int) extends Expr
class Var(val name : String) extends Expr
class Mul(val left : Expr, val right : Expr) extends Expr

object Num {
  def apply(value : Int) = new Num(value)
  def unapply(n : Num) = Some(n.value)
}

object Var {
  def apply(name : String) = new Var(name)
  def unapply(v : Var) = Some(v.name)
}

object Mul {
  def apply(left : Expr, right : Expr) = new Mul(left, right)
  //def apply() = new Mul(Num(1), Num(2))
  def unapply(m : Mul) = Some (m.left, m.right)
}
*/

trait Action

trait Thing

trait UrlAdressable {
  def getUrl = ""
}

trait Identifyable {
  def getId = ""
}

trait Store {
}

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
case class Update(val item : Thing with Identifyable, val in   : Store)  extends Action
case class Remove(val item : Thing with Identifyable, val from : Store)  extends Action





/**
 * Created with IntelliJ IDEA.
 * User: james
 * Date: 27/06/14
 * Time: 21:00
 * To change this template use File | Settings | File Templates.
 */
object Local extends App {

  /*
  val expr = Mul(Num(12), Num(1));

  val simplified = expr match {
    case Mul(x, Num(1)) => x
  }
  */




  implicit val system = ActorSystem("LocalSystem")
  val localActor = system.actorOf(Props[LocalActor], name = "LocalActor")  // the local actor
  localActor ! "START"                                                     // start the action


  localActor !  Add(item=Context("context name"), to=Profile("http://profile.daniel.de"))
  localActor !  Add(item=Item("item name"), to=Context("context name"))

}

