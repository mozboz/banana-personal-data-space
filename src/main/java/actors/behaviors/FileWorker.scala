package actors.behaviors

import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

import akka.actor.ActorRef
import requests._
import utils.BufferedResource

trait FileWorker extends WorkerActor {

  private val _file = new BufferedResource[String, FileChannel]("File")


  def handleFileRequests: Receive = new Receive {
    def isDefinedAt(x: Any) = x match {
      case x: ReadFile => true
      case x: WriteFile => true
      case x: FlushFile => true
      case _ => false
    }

    def apply(x: Any) = x match {
      case x: ReadFile => handle[ReadFile](sender(), x, handleReadFile)
      case x: WriteFile => handle[WriteFile](sender(), x, handleWriteFile)
      case x: FlushFile => handle[FlushFile](sender(), x, handleFlushFile)
      case _ => throw new Exception("This function is not applicable to objects of type: " + x.getClass)
    }
  }

  def startFileWorker(sender: ActorRef, message: Start, started: () => Unit) {

    readConfig("fileName", value => {
      val fileName = value.asInstanceOf[String]
      _file.set((key, channel, ex) => {
        val file = new RandomAccessFile(fileName, "rw")
        channel(file.getChannel)
        started()
      })
    },
    exception => throw exception)
  }

  def stopFileWorker(sender: ActorRef, message: Stop, stopped: () => Unit) {
    _file.withResource(channel => {
      channel.force(true)
      channel.close()
      stopped()
    }, ex => {
      stopped() // @todo: !! handle errors on file close
    })
  }

  private def withBuffer(message: FileMessage,
                         withBuffer:(MappedByteBuffer) => Unit,
                         error:(Exception) => Unit) {
    _file.withResource(
      (channel) => withBuffer(
        channel.map(FileChannel.MapMode.READ_WRITE, message.getOffset, message.getLength)
      ),
      (exception) => throw exception)
  }

  private def handleReadFile(sender: ActorRef, message: ReadFile) {
    withBuffer(message, buffer => {
        buffer.load()
        sender ! ReadFileResponse(message, buffer)
      },
      exception => throw exception)
  }

  private def handleWriteFile(sender: ActorRef, message: WriteFile) {
    withBuffer(message, buffer => {
      message.data.rewind()
      buffer.put(message.data)
      sender ! WriteFileResponse(message)
    },
    exception => throw exception)
  }

  private def handleFlushFile(sender: ActorRef, message: FlushFile) {
    _file.withResource((channel) => {
      channel.force(true)
      sender ! FlushFileResponse(message)
    },
    exception => throw exception)
  }
}