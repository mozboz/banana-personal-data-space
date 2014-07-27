package actors.behaviors

import java.util.UUID

class Response(requestId:UUID) extends Message {
  def getRequestId() : UUID = {
    requestId
  }
}

object Response {
  def unapply(response:Response) : Option[UUID] = {
    Some(response.messageId)
  }
}

/*
trait Response extends Message {
  private var _requestId : UUID = null

  def setRequestId(messageId:UUID) {
    _requestId = messageId
  }

  def requestId = _requestId
}*/