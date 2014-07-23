package actors.workers

import scala.util.control.Breaks._
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

import actors.behaviors.{Request, MessageHandler, RequestResponder}
import actors.workers.models.{KeyMapIndexEntry, KeyMapIndex}
import akka.actor.Actor
import akka.event.LoggingReceive
import events.{ConnectFile, DisconnectFile}
import requests._
import utils.BufferedResource

class FilesystemContextActor extends Actor with RequestResponder
                                      with MessageHandler {

  private val _fileChannelResource = new BufferedResource[String, FileChannel]("File")

  private val _contextIndex = new KeyMapIndex(context.self.path.name)

  // @todo: only for testing - replace with ConfigurationActor support
  context.self ! ConnectFile("/home/daniel/profileSystem/" + context.self.path.name + ".txt")

  def receive = LoggingReceive({

    case x:ConnectFile => _fileChannelResource.set((a,b,c) => b.apply(new RandomAccessFile(x.path, "rw").getChannel))
    case x:DisconnectFile => _fileChannelResource.reset(Some((channel) => channel.close()))
    // @todo: There should be a way to notify the caller about the failure of the clean-up action (Request/Response?)

    case x:Request => handleRequest(x, sender(), {

      case x:ReadFromContext =>
        readFromDataFile(x.key,
          (data) => {
            respond(x, ReadResponse(data, context.self.path.name))
          },
          (error) => respond(x, ErrorResponse(error)))


      case x:WriteToContext =>
        appendToDataFile(x.key, x.value,
          (indexEntry) => _contextIndex.add(indexEntry),
          // @todo: There should be two types of write response: 'accepted' and 'written'
          // the last should be sent here 'whenWritten'
          (exception) => respond(x, ErrorResponse(exception))
        )
        // @todo: There should be two types of write response: 'accepted' and 'written'
        // where the first would be here
        respond(x, WriteResponse())
    })
  })

  /**
   * Uses the actor's _fileChannelResource to create a MappedByteBuffer which is then
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

    _fileChannelResource.withResource(
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

    val paddedBytes = new Array[Byte](_contextIndex.padBytes(bytes.length))
    val blocks = _contextIndex.pad(paddedBytes.length)
    val targetAddress = _contextIndex.getNextAddressBytes

    val contextIndexEntry = new KeyMapIndexEntry(
      key = key,
      address = _contextIndex.pad(_contextIndex.getNextAddressBytes),
      length = blocks)

    indexEntry(contextIndexEntry)

    // @todo: Find a way to avoid array copy (ByteBuffer?)
    bytes.copyToArray(paddedBytes)

    withBuffer(targetAddress, paddedBytes.length,
      (buffer) => {
        buffer.put(paddedBytes)
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
    val range = _contextIndex.getRangeBytes(key)

    withBuffer(range._1, range._2,
      (buffer) => {
        val paddedData = new Array[Byte](range._2)

        buffer.get(paddedData)

        var data = paddedData

        // @todo: Find a more elegant way to tell the content size of a block (header?, map of partial blocks?)
        breakable {
          for ((x, i) <- paddedData.view.zipWithIndex) {
            if (x == 0) {
              data = paddedData.slice(0, i)
              break()
            }
          }
        }

        withData(new String(data, "UTF-8"))
      },
      (exception) => error(exception)
    )
  }
}