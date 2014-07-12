package actors.supervisors

import java.util.UUID

trait Message {
  private val _messageId = UUID.randomUUID()
  def messageId = _messageId
}