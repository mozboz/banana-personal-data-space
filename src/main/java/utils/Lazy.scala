package utils

import actors.behaviors.Resource

import scala.collection.mutable

/**
 * Deals with the behavior of lazy resources.
 * @tparam TKey The type of the resource identifier
 * @tparam TResource The type of the resource
 */
trait Lazy[TKey,TResource] extends Resource[TResource] {

  private val _resourceRequests = new mutable.Queue[((TResource) => Unit, (Exception) => Unit)]
  private val _resource = new ValueContainer[TResource]

  private val _available = new ValueContainer[Boolean](Some(false))
  private val _failState = new ValueContainer[Boolean](Some(false))
  private val _initialized = new ValueContainer[Boolean](Some(false))

  /**
   * Initializes the lazy resource with a key and the loader function.
   * @param resourceKey The resource key
   * @param loadResource The resource loader function with a success and error continuation as parameters
   */
  def init(resourceKey: TKey,
           loadResource : (TKey, (TResource) => Unit, (Exception) => Unit) => Unit) {
    HandleFailState()

    if (!_initialized.get().getOrElse(false)) {
      resourceLoader(resourceKey, loadResource)
      _initialized.set(true)
    }
  }

  /**
   * Executes an action with the resource as parameter
   * @param withResource Executes with the resource as parameter
   * @param onError Executes with the exception as parameter
   */
  def withResource(withResource : (TResource) => Unit,
                   onError : (Exception) => Unit) {
    HandleFailState()
    executeWhenAvailable(withResource, onError)
  }

  /**
   * Unloads the resource and forces a reload of the resource on the next enqueued action
   */
  def reset() {
    _available.set(false)
    _failState.set(false)
    _initialized.set(false)
  }

  /**
   * Enqueues or executes an action.
   * @param withResource The action to apply when the resource is ready.
   */
  private def executeWhenAvailable(withResource: (TResource) => Unit, onError: (Exception) => Unit) {
    if (_available.get().getOrElse(false))
      withResource.apply(_resource.get().get)
    else
      _resourceRequests.enqueue(Tuple2(withResource, onError))
  }

  private def HandleFailState() {
    if (_failState.get().getOrElse(false))
      throw new Exception("The resource is in fail state")
  }

  /**
   * Wrapper around the actual loader function. Used to catch errors.
   * @param resourceKey The resource key
   * @param actualLoader The actual loader function
   */
  private def resourceLoader (resourceKey: TKey, actualLoader: (TKey, (TResource) => Unit, (Exception) => Unit) => Unit) {
    try {
      actualLoader.apply(resourceKey, resourceLoaded, resourceLoadingError)
    } catch {
      case e:Exception => resourceLoadingError(e)
    }
  }

  /**
   * Must be called from within the loadResource action when the resource was loaded
   * @param resource The resource
   */
  private def resourceLoaded(resource: TResource) {
    HandleFailState()

    _available.set(true)
    _resource.set(resource)

    _resourceRequests.dequeueAll(x => {
      x._1.apply(resource)
      true
    })
  }

  /**
   * Must be called when there is an error while resource loading.
   * @param ex The exception
   */
  private def resourceLoadingError (ex:Exception) {
    _failState.set(true)

    _resourceRequests.dequeueAll(x => {
      x._2.apply(ex)
      true
    })
  }
}