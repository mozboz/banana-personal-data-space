package actors.behaviors

import java.util.UUID

trait Response extends Message {
  private var _requestId : UUID = null

  def setRequestId(messageId:UUID) {
    _requestId = messageId
  }

  def requestId = _requestId
}