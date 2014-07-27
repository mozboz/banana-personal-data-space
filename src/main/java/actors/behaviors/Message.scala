package actors.behaviors

import java.util.UUID

/**
 * Trait which extends a class with the necessary features to be handled as a message.
 */
trait Message {
  private val _messageId = UUID.randomUUID()
  def messageId = _messageId
}