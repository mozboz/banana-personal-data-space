package actors.behaviors

import java.util.UUID

trait Request extends  Message {
}

object Request
{
  def unapply(request:Request) : Option[UUID] = {
    Some(request.messageId)
  }
}
