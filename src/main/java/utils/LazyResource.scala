package utils

import utils.Lazy

/**
 * Default lazy resource implementation
 * @param key The key of the context
 */
class LazyResource[TKey, TResource](key: TKey)
  extends Lazy[TKey,TResource] {

  def set(loader: (TKey, (TResource) => Unit, (Exception) => Unit) => Unit) = {
    init(key, loader)
  }
}