package actors.workers.models

import scala.collection.mutable


// @todo: Improve the index:
// * implement as linked list (helps to maintain the sort order without
//   moving large parts of the data on insert. New entries can be just appended.
// * Use this class as frontend and create a corresponding backend which
//   operates on memory-mapped-files for large indexes.


/**
 * Maps a key to a specific position in a file. Can be used as a primary key to which other keys can reference.
 * @param name The name of the index
 */
class KeyMapIndex(name:String) {

  private val _entries = new mutable.HashMap[String, KeyMapIndexEntry]
  private var _lastEntry : Option[KeyMapIndexEntry] = None

  private val _blockSize = 1 // @todo: evaluate if necessary


  def getBlockSize : Int = {
    _blockSize
  }

  def getName : String = {
    name
  }

  def add(entry: KeyMapIndexEntry) {
    if (_entries.contains(entry.key))
      throw new Exception("The key '" + entry.key + "' already exists in index '" + name + "'.")

    _entries.put(entry.key, entry)
    _lastEntry = Some(entry)
  }

  /**
   * Converts bytes to blocks and adds a padding if necessary
   * @param bytes The number of bytes
   * @return The padded length in blocks
   */
  def pad(bytes:Int) : Int = {

    if (_blockSize == 1)
      return bytes

    val blocks = bytes / _blockSize

    if (bytes % _blockSize > 0)
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

    if (_blockSize == 1)
      return bytes

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
    val entry = _entries.getOrElse(key, null)

    if (entry == null)
      throw new Exception("The key '" + key + "' is not present in index '" + name + "'.")

    (entry.address,entry.length)
  }

  /**
   * Returns the same as "getRange" but uses bytes as unit.
   * @param key The key
   * @return A tuple (address,length) both in bytes
   */
  def getRangeBytes(key:String) : (Int,Int) = {
    val range = getRange(key)

    (range._1 * _blockSize, range._2 * _blockSize)
  }

  /**
   * Iterates over all index entries.
   * @param handler The handler
   */
  def foreachEntry (handler:(KeyMapIndexEntry) => Unit) {
    _entries.foreach((a) => handler(a._2))
  }
}