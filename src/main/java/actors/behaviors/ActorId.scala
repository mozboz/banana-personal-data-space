package actors.behaviors

import java.util.UUID

trait ActorId {
  private val _actorId = UUID.randomUUID()
  def actorId = _actorId
}
