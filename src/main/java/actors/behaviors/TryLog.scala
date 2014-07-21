package actors.behaviors

import akka.event.LoggingAdapter

trait TryLog {
  private var _loggingAdapter : LoggingAdapter = null

  def setLoggingAdapter(log:LoggingAdapter) {
    _loggingAdapter = log
  }

  def tryLog(x:(LoggingAdapter) => Unit) {
    if (_loggingAdapter != null)
      x.apply(_loggingAdapter)
  }
}




