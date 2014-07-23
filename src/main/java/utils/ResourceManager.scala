package utils

import scala.collection.mutable

/**
 * Can be used to manage all kinds of objects as resource.
 * @param factory The factory function which is used to load or create the resources. Takes a success and error continuation-parameter.
 * @tparam TKey The type of the resource's key
 * @tparam TResource The resource type
 */
final class ResourceManager[TKey, TResource](factory: (TKey, (TResource) => Unit, (Exception) => Unit) => Unit) {

  private val _managedResources = new mutable.HashMap[TKey, BufferedResource[TKey, TResource]]

  /**
   * Gets the resource
   * @param resourceKey The resource's key
   */
  def get(resourceKey: TKey) : BufferedResource[TKey, TResource] = {

    if (_managedResources.contains(resourceKey))
      return _managedResources.get(resourceKey).get

    val lazyResource = new BufferedResource[TKey, TResource](resourceKey)
    lazyResource.set(factory)

    _managedResources.put(resourceKey, lazyResource)

    lazyResource
  }

  /**
   * Resets the resource
   * @param resourceKey The resource's key
   */
  def reset(resourceKey: TKey) {
    if (!_managedResources.contains(resourceKey))
      throw new Exception("The resource with the key " + resourceKey.toString + " is not managed by this ResourceManager.")

    _managedResources.get(resourceKey).get.reset(None)
  }

  /**
   * Gets a Iterable for the resource-keys
   */
  def keys() : Iterable[TKey] = {
    _managedResources.keys
  }
}