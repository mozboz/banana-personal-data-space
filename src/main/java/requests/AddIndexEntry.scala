package requests

import actors.behaviors.{Response, Request}
import actors.workers.IndexEntry


case class AddIndexEntry(entry:IndexEntry) extends Request
case class AddIndexEntryResponse(request:Request) extends Response(request.messageId)