package utils

final class ValueContainer[T] (initial : Option[T] = None) {
  private var _value : Option[T] = initial

  def set(value:T) {
    _value = Some(value)
  }

  def get() : Option[T] = {
    _value
  }
}
