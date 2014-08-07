package actors.workers

import java.io.RandomAccessFile
import java.nio.channels.FileChannel

import actors.behaviors.WorkerActor
import akka.actor.ActorRef
import requests._
import utils.BufferedResource


class Index extends WorkerActor {

  private var _folder = ""
  private val _file = new BufferedResource[String, FileChannel]("File")
  def name = context.parent.path.name

  def handleRequest = {
    case x: AddIndexEntry => handle[AddIndexEntry](sender(), x, addIndexEntry)
    case x: GetEntry => handle[GetEntry](sender(), x, getEntry)
    case x: GetNextAddress => handle[GetNextAddress](sender(), x, getNextAddress)
  }

  def start(sender: ActorRef, message: Start, started: () => Unit) {
    request[ReadConfigResponse](ReadConfig("dataFolder"), message.configRef,
      (response) => {
        _folder = response.value.asInstanceOf[String]
        _file.set((key, channel, ex) => {
          val file = new RandomAccessFile(_folder + name + ".idx", "rw")
          channel(file.getChannel)
        })
        started()
      },
      (ex) => throw ex)
  }

  def stop(sender: ActorRef, message: Stop, stopped: () => Unit) {
    _file.reset(Some((channel) => channel.close()))
    stopped()
  }

  def addIndexEntry(sender: ActorRef, message: AddIndexEntry) {
  }

  def getEntry(sender: ActorRef, message: GetEntry) {
  }

  def getNextAddress(sender: ActorRef, message: GetNextAddress) {
  }
}

trait ListItem extends Iterable[ListItem] {

  private var _prev: ListItem = Empty
  private var _next: ListItem = Empty

  def getPrevious: ListItem = {
    _prev
  }

  def setPrevious(prev: ListItem) {
    _prev = prev
  }

  def getNext: ListItem = {
    _next
  }

  def setNext(next: ListItem) {
    _next = next
  }

  def iterator = Iterator.iterate(this)(_.getNext)
  def iteratorReverse = Iterator.iterate(this)(_.getPrevious)
}

object Empty extends ListItem

class IndexEntryList {

  private var _first : ListItem = Empty
  private var _last : ListItem = Empty
  private var _count = 0

  def getFirst : ListItem = {
    _first
  }

  def getLast : ListItem = {
    _last
  }

  def getCount : Int = {
    _count
  }

  def append(item:ListItem) {
    insert(item, _last)
  }

  def prepend(item:ListItem) {
    insert(item, Empty)
  }

  def insert(item:ListItem, after:ListItem) {

    if (_first == Empty) {
      // first insert
      _first = item
      _last = item
    } else {
      if (after == Empty) {
        // prepend
        item.setNext(_first)
        _first.setPrevious(item)
        _first = item
      } else {
        // insert (or append)
        item.setPrevious(after)
        item.setNext(after.getNext)
        after.setNext(item)

        if (_last == after) // append
          _last = item
      }
    }

    _count = _count + 1
  }

  def remove(item:ListItem) {

    if (_first == _last) {
      _first = Empty
      _last = Empty
    } else {
      if (item == _first) {
        _first = item.getNext
        _first.setPrevious(Empty)
      } else if (item == _last) {
        _last = _last.getPrevious
        _last.setNext(Empty)
      } else {
        item.getPrevious.setNext(item.getNext)
      }
    }

    _count = _count - 1
  }
}

/**
 * Wraps a IndexEntry so that it can be used in a linked list.
 * @param indexEntry The original index entry
 */
class IndexEntryListItem(indexEntry:IndexEntry) extends ListItem

case class IndexEntry(key: String, offset: Int, length: Int) {

  private val _keyUtf8 = key.getBytes("UTF-8")

  if (_keyUtf8.length > 4074)
    throw new Exception("The key size is not allowed to exceed 4074 bytes (UTF-8)")

  def getKeyBytes: Array[Byte] = {
    val padded = new Array[Byte](4074)
    _keyUtf8.copyToArray(padded)
    padded
  }

  def getKeyLength: Short = {
    _keyUtf8.length.toShort
  }
}