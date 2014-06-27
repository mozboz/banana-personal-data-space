package actors

import akka.actor.Actor
import messages.{CreateContext, Add}
import com.sun.xml.internal.bind.CycleRecoverable.Context
import com.sun.xml.internal.ws.resources.AddressingMessages

class MasterActor extends Actor {

  sealed abstract class Expression
  sealed abstract class Addable(key: String)



  case class X() extends Expression
  case class Context(key: String) extends Addable(key)
  case class Content(key: String, value: String) extends Addable(key)
  case class Const(value : Int) extends Expression
  case class Add(objectType : Addable, name : String) extends Expression


  def receive = {
    case Add(item: Addable, to: String) => sender ! "omg hai"
    // case Add(object: ContextItem, name: String, value: String) =>
  }
}


object ObjectType extends Enumeration {
  type Action = Value
  val CONTEXT, CONTEXTITEM = Value
}

