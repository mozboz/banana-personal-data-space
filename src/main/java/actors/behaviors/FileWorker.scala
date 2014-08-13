package actors.behaviors

import java.io.RandomAccessFile
import java.nio.channels.FileChannel

import akka.actor.ActorRef
import requests._
import utils.BufferedResource

trait FileWorker extends WorkerActor {

  private val _file = new BufferedResource[String, FileChannel]("File")


  def handleFileRequests: Receive = new Receive {
    def isDefinedAt(x: Any) = x match {
      case x:ReadFile => true
      case x:WriteFile => true
      case _ => false
    }
    def apply(x: Any) = x match {
      case x:ReadFile => handle[ReadFile](sender(), x, handleReadFile)
      case x:WriteFile => handle[WriteFile](sender(), x, handleWriteFile)
      case _ => throw new Exception("This function is not applicable to objects of type: " + x.getClass)
    }
  }

  def startFileBoundWorker(sender:ActorRef, message:Start, started:() => Unit) {
    request[ReadActorConfigResponse](
      ReadActorConfig(actorId, "fileName"), message.configRef,
      (response) => {
        val fileName = response.value.asInstanceOf[String]

        _file.set((key, channel, ex) => {
          val file = new RandomAccessFile(fileName, "rw")
          channel(file.getChannel)

          started()
        })
      },
      (exception) => throw exception)
  }

  private def handleReadFile(sender:ActorRef, message:ReadFile) {

  }

  private def handleWriteFile(sender:ActorRef, message:WriteFile) {

  }
}