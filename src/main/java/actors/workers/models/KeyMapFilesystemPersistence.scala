package actors.workers.models

import java.io.RandomAccessFile
import java.nio.ByteBuffer

object KeyMapFilesystemPersistence {

  def load(folder:String, indexName:String) : KeyMapIndex = {

    val index = new KeyMapIndex(indexName)

    // @todo: Do proper path handling
    var file : RandomAccessFile = null

    try {
      file = new RandomAccessFile(folder + indexName, "r")
    } catch {
      case x:Exception => return index
    }

    val channel = file.getChannel

    while (channel.position < channel.size) {
      val entryBuffer = ByteBuffer.allocate(4096)
      channel.read(entryBuffer)
      entryBuffer.flip()
      val entry = entryFromBuffer(entryBuffer)
      index.add(entry)
    }

    index
  }

  def save(index:KeyMapIndex, folder:String) {

    // @todo: Do proper path handling
    val file = new RandomAccessFile(folder + index.getName, "rw")
    val channel = file.getChannel

    index.foreachEntry((entry) => {

      val entryBuffer = bufferFromEntry(entry)
      entryBuffer.flip()

      while (entryBuffer.hasRemaining)
        channel.write(entryBuffer)
    })

    channel.close()
  }

  private def bufferFromEntry(entry:KeyMapIndexEntry) : ByteBuffer = {
    val serializedEntry = ByteBuffer.allocate(4096)

    serializedEntry.putShort(entry.getKeyLength)
    serializedEntry.put(entry.getKeyBytes)
    serializedEntry.putInt(entry.address)
    serializedEntry.putInt(entry.length)
    serializedEntry.putLong(entry.getTimestamp.getTime)
    serializedEntry.putInt(entry.getAccessCount)

    serializedEntry
  }

  private def entryFromBuffer(buffer:ByteBuffer) : KeyMapIndexEntry = {

    val keyLength = buffer.getShort
    val keyBytes = new Array[Byte](keyLength)
    buffer.get(keyBytes)
    val keyString = new String(keyBytes, "UTF-8")
    buffer.position(4076)
    val address = buffer.getInt
    val length = buffer.getInt
    val timestamp = buffer.getLong
    val accessCount = buffer.getInt

    val entry = new KeyMapIndexEntry(keyString,address,length)
    entry.setTimestamp(timestamp)
    entry.setAcessCount(accessCount)

    entry
  }
}
