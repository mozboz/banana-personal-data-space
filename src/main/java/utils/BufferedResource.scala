package utils

/**
 * Default lazy resource implementation
 * @param key The key of the context
 */
class BufferedResource[TKey, TResource](key: TKey)
  extends Buffer[TKey,TResource] {

  def set(loader: (TKey, (TResource) => Unit, (Exception) => Unit) => Unit) = {
    init(key, loader)
  }
}