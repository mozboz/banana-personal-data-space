package actors.behaviors

/**
 * When implemented, provides access to a resource.
 * @tparam TResource The type of the resource
 */
trait Resource[TResource] {
  /**
   * Executes either the supplied action using the resource or
   * calls the onError function.
   * @param withResource Executes with the resource as parameter
   * @param onError Executes with the exception as parameter
   */
  def withResource(withResource : (TResource) => Unit,
                   onError : (Exception) => Unit)
}
