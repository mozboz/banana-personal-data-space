package actors.workers.models

import scala.collection.mutable

class KeyMapIndex {

  private val _entries = new mutable.HashMap[String, KeyMapIndexEntry]
  private var _lastEntry : Option[KeyMapIndexEntry] = None

  private val _blockSize = 1024

  def add(entry: KeyMapIndexEntry) {
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

    if (remainder > 0)
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
    // @todo: Add error handling for the case that the key doesn't exist

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