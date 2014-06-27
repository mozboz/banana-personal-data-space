package actors

object Action extends Enumeration {
  type Action = Value
  val ADD, DELETE, GET = Value
}
