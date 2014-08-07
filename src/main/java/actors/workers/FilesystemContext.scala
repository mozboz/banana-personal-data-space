package actors.workers

import java.io.RandomAccessFile

import scala.util.control.Breaks._
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

import actors.behaviors._
import actors.workers.models.{KeyMapFilesystemPersistence, KeyMapIndexEntry, KeyMapIndex}
import akka.actor.ActorRef
import requests._
import utils.BufferedResource

class FilesystemContext extends WorkerActor {

  private val _file = new BufferedResource[String, FileChannel]("File")
  private var _index = new KeyMapIndex(indexName)
  private var _folder = ""

  // @todo: Don't use the actor path here, since it can not contain some characters which would be valid in a context name
  // @todo: Also don't use this as the filename
  def name = context.parent.path.name
  def indexName = name + ".idx"

  def handleRequest = {
    case x:Read => handle[Read](sender(), x, read)
    case x:Write => handle[Write](sender(), x, write)
  }

  def start(sender:ActorRef, message:Start, started:() => Unit) {
    request[ReadConfigResponse](ReadConfig("dataFolder"), message.configRef,
      (response) => {
        _folder = response.value.asInstanceOf[String]
        _index = KeyMapFilesystemPersistence.load(_folder, indexName)
        _file.set((key, channel, ex) => {
           val file = new RandomAccessFile(_folder + name, "rw")
          channel(file.getChannel)
        })
        started()
      },
      (ex) => throw ex)
  }

  def stop(sender:ActorRef, message:Stop, stopped:() => Unit) {
    // @todo: Implement stop
    stopped()
  }

  private def read(sender:ActorRef, message:Read) {
    readFromDataFile(message.key,
      (data) => sender ! ReadResponse(message, data),
      (ex) => throw ex)
  }

  def write(sender:ActorRef, message:Write) {
    appendToDataFile(message.key, message.value,
      (indexEntry) => {
        _index.add(indexEntry)

        // @todo: This is bullshit but works for now...
        KeyMapFilesystemPersistence.save(_index, _folder)

        sender ! WriteResponse(message)
      },
      // @todo: There should be two types of write response: 'accepted' and 'written'
      (ex) => throw ex)
  }

  /**
   * Uses the actor's _file to create a MappedByteBuffer which is then
   * supplied to the withBuffer-continuation.
   * @param position The position in the file
   * @param length The length of the buffer
   * @param withBuffer The success continuation
   * @param error The error continuation
   */
  private def withBuffer(position:Int,
                 length:Int,
                 withBuffer:(MappedByteBuffer) => Unit,
                 error:(Exception) => Unit) {
    _file.withResource(
        (channel) => withBuffer.apply(channel.map(FileChannel.MapMode.READ_WRITE, position, length)),
        (exception) => error(exception))
  }

  /**
   * Appends a value to the data file and creates a index entry for it.
   * @param key The key
   * @param data The data
   * @param indexEntry Is called when the corresponding index entry is available
   * @param error Is called in case of an error
   * @return A index entry
   */
  private def appendToDataFile(key:String,
                               data:String,
                               indexEntry: (KeyMapIndexEntry) => Unit,
                               error: (Exception) => Unit) {

    val bytes = data.getBytes("UTF-8")

    val paddedBytes = new Array[Byte](_index.padBytes(bytes.length))
    val blocks = _index.pad(paddedBytes.length)
    val targetAddress = _index.getNextAddressBytes

    val contextIndexEntry = new KeyMapIndexEntry(
      key = key,
      address = _index.pad(_index.getNextAddressBytes),
      length = blocks)

    indexEntry(contextIndexEntry)

    if (_index.getBlockSize != 1)
      // @todo: Find a way to avoid array copy (ByteBuffer?)
      bytes.copyToArray(paddedBytes)

    withBuffer(targetAddress, paddedBytes.length,
      (buffer) => {
        if (_index.getBlockSize != 1) // @todo: Is there something for what I would need the blocks? throw it out?!
          buffer.put(paddedBytes)
        else
          buffer.put(bytes)

        contextIndexEntry.setProcessed()
      },
      // @todo: Provide simple mechanism to wrap the exception with some information about the current method
      (exception) => error(exception)
    )
  }

  /**
   * Reads data from the data file.
   * @param key The key
   * @return The data
   */
  private def readFromDataFile (key:String, withData:(String) => Unit, error:(Exception) => Unit) {
    val range = _index.getRangeBytes(key)

    withBuffer(range._1, range._2,
      (buffer) => {
        val paddedData = new Array[Byte](range._2)

        buffer.get(paddedData)

        var data = paddedData

        if (_index.getBlockSize != 1) {
          // @todo: Find a more elegant way to tell the content size of a block (header?, map of partial blocks?)
          breakable {
            for ((x, i) <- paddedData.view.zipWithIndex) {
              if (x == 0) {
                data = paddedData.slice(0, i)
                break()
              }
            }
          }
        }

        withData(new String(data, "UTF-8"))
      },
      (exception) => error(exception)
    )
  }
}