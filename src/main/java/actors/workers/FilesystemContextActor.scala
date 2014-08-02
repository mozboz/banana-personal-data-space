package actors.workers

import scala.util.control.Breaks._
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

import actors.behaviors._
import actors.workers.models.{KeyMapIndexEntry, KeyMapIndex}
import akka.actor.ActorRef
import requests._
import utils.BufferedResource

class FilesystemContextActor extends WorkerActor {

  private val _file = new BufferedResource[String, FileChannel]("File")
  private var _index = new KeyMapIndex(context.self.path.name)
  private var _folder = ""

  def handleRequest = {
    case x:Read => read(sender(), x)
    case x:Write => write(sender(), x)
  }

  def start(sender:ActorRef, message:Start, started:() => Unit) {
    /*
    // @todo: integrate the backend-actor into the initialization-hierarchy by adding it as a child
    onResponseOf(GetContextDataFilePath(context.self.path.name), message.configRef, self, {
      case x:GetContextDataFilePathResponse =>
        _folder = x.path
        self ! ConnectFile(_folder + context.self.path.name + ".txt")
        sender ! StartupResponse(message)

      case x:ErrorResponse => throw x.ex
    })*/
  }

  def stop(sender:ActorRef, message:Stop, stopped:() => Unit) {
    /*
    self ! DisconnectFile()
    sender ! StopResponse
    */
  }

  /*
  private def handleConnectFile(sender:ActorRef, message:ConnectFile) {
    _index = new KeyMapFilesystemPersistence().load(_folder, context.self.path.name)
    _file.set((a,b,c) => b.apply(new RandomAccessFile(message.path, "rw").getChannel))
  }

  private def handleDisconnectFile(sender:ActorRef, message:DisconnectFile) {
    _file.reset(Some((channel) => channel.close()))
    new KeyMapFilesystemPersistence().save(_index, _folder)
    // @todo: There should be a way to notify the caller about the failure of the clean-up action (Request/Response?)
  }
*/

  private def read(sender:ActorRef, message:Read) {
    readFromDataFile(message.key,
      (data) => sender ! ReadResponse(message, data),
      (error) => sender ! ErrorResponse(message, error))
  }

  def write(sender:ActorRef, message:Write) {
    appendToDataFile(message.key, message.value,
      (indexEntry) => try {
        _index.add(indexEntry)
      } catch {
        case x:Exception => sender ! ErrorResponse(message, x)
      },
      // @todo: There should be two types of write response: 'accepted' and 'written'
      // the last should be sent here 'whenWritten'
      (exception) => sender ! ErrorResponse(message, exception)
    )
    // @todo: There should be two types of write response: 'accepted' and 'written'
    // where the first would be here
    sender ! WriteResponse(message)
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