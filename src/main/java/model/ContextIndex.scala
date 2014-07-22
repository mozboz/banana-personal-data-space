package model

import java.util.{Date, Calendar}

import scala.collection.mutable

class ContextIndex {

  private val _entries = new mutable.HashMap[String, ContextIndexEntry]
  private var _lastEntry : Option[ContextIndexEntry] = None

  private val _blockSize = 1024

  def add(entry: ContextIndexEntry) {
    _entries.put(entry.key, entry)
    _lastEntry = Some(entry)
  }

  /**
   * Converts bytes to blocks and adds a padding if necessary
   * @param bytes The number of bytes
   * @return The padded length in blocks
   */
  def pad(bytes:Int) : Int = {

    val blocks = bytes / _blockSize
    val remainder = bytes % _blockSize

    return if (remainder > 0)
      blocks + 1
    else
      blocks
  }

  /**
   * Pads the supplied bytes so that it fills up complete blocks.
   * @param bytes The number of bytes to pad
   * @return The padded number of bytes
   */
  def padBytes(bytes:Int) : Int = {
    if (bytes <= _blockSize)
      _blockSize
    else
      pad(bytes) * _blockSize
  }

  /**
   * Gets the next free address in the file (in blocks).
   * @return The next free address in blocks
   */
  def getNextAddress : Int = {
    var address = 0
    var length = 0

    if (_lastEntry.isDefined) {
      address = _lastEntry.get.address
      length = _lastEntry.get.length
    }

    address+length
  }

  /**
   * Gets the next free address in the file (in byte).
   * @return The next free address in byte
   */
  def getNextAddressBytes : Int = {
    getNextAddress * _blockSize
  }

  /**
   * Returns the position of a specified entry in the data file
   * @param key The key
   * @return A tuple (address,length) both in blocks
   */
  def getRange(key:String) : (Int, Int) = {
    val entry = _entries.get(key).get
    new Tuple2[Int,Int](entry.address,entry.length)
  }

  /**
   * Returns the same as "getRange" but uses bytes as unit.
   * @param key The key
   * @return A tuple (address,length) both in bytes
   */
  def getRangeBytes(key:String) : (Int,Int) = {
    val range = getRange(key)
    new Tuple2[Int,Int](range._1 * _blockSize, range._2 * _blockSize)
  }
}

/**
 * Represents a index entry which points to a block in the ContextData.
 * @param key The key as UTF-8 string representation
 * @param address The address of the corresponding entry in the data file in blocks
 * @param length The length of the entry in the data file in blocks
 */
case class ContextIndexEntry(key:String, address:Int, length : Int) {

  private val _keyUtf8 = key.getBytes("UTF-8")

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