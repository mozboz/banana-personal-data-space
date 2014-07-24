package actors.workers.models

import java.nio.{ByteBuffer, ShortBuffer}
import java.util.{Calendar,Date}

/**
 * Represents a index entry which points to a block in the context data.
 * @param key The key as UTF-8 string representation
 * @param address The address of the corresponding entry in the data file in blocks
 * @param length The length of the entry in the data file in blocks
 */
case class KeyMapIndexEntry(key:String, address:Int, length : Int) {

  private val _keyUtf8 = key.getBytes("UTF-8")

  if (_keyUtf8.length > 4074)
    throw new Exception("The key size is not allowed to exceed 4074 bytes (UTF-8)")

  private var _processed = false

  def setProcessed() {
    _processed = true
  }

  def isProcessed : Boolean = {
    _processed
  }

  def getKeyBytes : Array[Byte] = {
     val padded = new Array[Byte](4074)
     _keyUtf8.copyToArray(padded)
    padded
  }

  def getKeyLength : Short = {
    _keyUtf8.length.toShort
  }

  private val _timestamp = Calendar.getInstance.getTime

  def getTimestamp : Date = {
    _timestamp
  }

  def setTimestamp(timestamp : Long) {
    _timestamp.setTime(timestamp)
  }

  private var _accessCount = 0

  def getAccessCount : Int = {
    _accessCount
  }

  // @todo: implement access counter
  def setAcessCount (accessCount: Int) {
    _accessCount = accessCount
  }
}
