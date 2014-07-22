package actors.workers

import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

import actors.behaviors.{Request, MessageHandler, RequestResponder}
import akka.actor.Actor
import akka.event.LoggingReceive
import events.{ConnectFile, DisconnectFile}
import model.{ContextIndex, ContextIndexEntry}
import requests.{WriteResponse, WriteToContext, ReadResponse, ReadFromContext}
import utils.BufferedResource

class LocalContextActor extends Actor with RequestResponder
                                      with MessageHandler {

  private val _fileChannelResource = new BufferedResource[String, FileChannel]("File")

  private val _contextIndex = new ContextIndex

  // @todo: only for testing
  context.self ! ConnectFile("/home/daniel/profileSystem/" + context.self.path.name + ".txt")

  def receive = LoggingReceive({

    case x:ConnectFile => _fileChannelResource.set((a,b,c) => b.apply(new RandomAccessFile(x.path, "rw").getChannel))
    case x:DisconnectFile => _fileChannelResource.reset(Some((channel) => channel.close()))

    case x:Request => handleRequest(x, sender(), {

      case x:ReadFromContext => respond(x, ReadResponse("bla", context.self.path.name))

      case x:WriteToContext =>
        _contextIndex.add(appendToDataFile(x.key, x.value))
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
  def withBuffer(position:Int,
                 length:Int,
                 withBuffer:(MappedByteBuffer) => Unit,
                 error:(Exception) => Unit) {

    _fileChannelResource.withResource(
        (channel) => withBuffer.apply(channel.map(FileChannel.MapMode.READ_WRITE, position, length)),
        (exception) => error(exception))
  }

  /**
   * Appends a value to the data file and indexes it.
   * @param key The key
   * @param data The data
   * @return A index entry
   */
  def appendToDataFile(key:String, data:String) : ContextIndexEntry = {

    val bytes = data.getBytes("UTF-8")
    val appendPosition = _contextIndex.getNextAddressBytes
    val appendBlock = _contextIndex.pad(_contextIndex.getNextAddressBytes)
    val blocks = _contextIndex.pad(bytes.length)
    val paddedLength = _contextIndex.padBytes(bytes.length)

    val contextIndexEntry = new ContextIndexEntry(key, appendBlock, blocks)

    withBuffer(appendPosition, paddedLength,
      (buffer) => buffer.put(bytes),
      (exception) => throw exception
    )

    contextIndexEntry
  }

  def readFromFile () {

  }
}