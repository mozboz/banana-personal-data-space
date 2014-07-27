package actors.behaviors

import java.util.UUID

/**
 * Base class for all responses.
 * @param requestId The id of the request this response replies to
 */
abstract class Response(requestId:UUID) extends Message {
  def getRequestId: UUID = {
    requestId
  }
}

object Response {
  def unapply(response:Response) : Option[UUID] = {
    Some(response.getRequestId)
  }
}