package actors.supervisors

/**
 * Owns a group of contexts.
 * * Interface
 *   Handles events:
 *   * PropagateProfile: Sets the profile which is responsible for this context group owner.
 *
 *   Responds to following requests:
 *   * SpawnContext: Spawns the requested context if possible and returns a actorRef
 *   * ContextExists: Proxies the received request to the profile actor and then responds with the result
 *
 *   Issues the following requests:
 *   *
 *
 *   Proxies the following requests:
 *   * ContextExists: Proxies the received request to the profile actor and then responds with the result
 */
class ContextGroupOwnerActor {

}