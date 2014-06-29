package messages

case class Add   (val item : Thing with Identifyable, val to   : Store)  extends Action
case class Update(val item : Thing with Identifyable, val in   : Store)  extends Action
case class Remove(val item : Thing with Identifyable, val from : Store)  extends Action