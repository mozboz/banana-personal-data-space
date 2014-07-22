package actors.workers.models

import java.util.{Calendar,Date}

/**
 * Represents a index entry which points to a block in the context data.
 * @param key The key as UTF-8 string representation
 * @param address The address of the corresponding entry in the data file in blocks
 * @param length The length of the entry in the data file in blocks
 */
case class KeyMapIndexEntry(key:String, address:Int, length : Int) {

  private val _keyUtf8 = key.getBytes("UTF-8")

  private var _processed = false

  def setProcessed() {
    _processed = true
  }

  def getProcessed : Boolean = {
    _processed
  }

  def getKeyBytes : Array[Byte] = {
    _keyUtf8
  }

  def getKeyLength : Int = {
    _keyUtf8.length
  }

  private val _timestamp = Calendar.getInstance.getTime

  def getTimestamp : Date = {
    _timestamp
  }

  private var _accessCount = 0

  def getAcessCount : Int = {
    _accessCount
  }

  def setAcessCount (accessCount: Int) {
    _accessCount = accessCount
  }
}
