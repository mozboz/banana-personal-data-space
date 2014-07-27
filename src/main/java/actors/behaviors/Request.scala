package actors.behaviors

import java.util.UUID

/**
 * Trait which marks a request which can be answered by a actors.behaviours.Response.
 */
trait Request extends  Message

object Request
{
  def unapply(request:Request) : Option[UUID] = {
    Some(request.messageId)
  }
}