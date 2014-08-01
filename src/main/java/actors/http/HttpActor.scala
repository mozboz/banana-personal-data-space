package actors.http

import akka.util.Timeout
import akka.actor._
import spray.can.Http
import spray.http._
import HttpMethods._
import MediaTypes._
import requests._
import actors.behaviors.WorkerActor
import requests.SetProfileAccessor
import requests.ErrorResponse

class HttpActor extends WorkerActor {

  implicit val timeout: Timeout = 1 // for the actor 'asks'
  import context.dispatcher // ExecutionContext for the futures and scheduler
  var profileAccessor: ActorRef = _

  def start(sender:ActorRef, message:Start) {

  }

  def stop(sender:ActorRef, message:Stop) {

  }

  def handleRequest =  {
    // when a new connection comes in we register ourselves as the connection handler
    case _: Http.Connected => sender ! Http.Register(self)

    case HttpRequest(GET, Uri.Path(path), _, _, _) =>
      val s = sender()

      val req = Read(path.stripPrefix("/"), "Context2")

      //onResponseOf(req ,profileAccessor,self,  {

      aggregateOne(req, profileAccessor, (response, sender, done) => {
        response match {
        case x:ReadResponse => s ! HttpResponse(entity = HttpEntity(`text/html`, x.data))
        case x:ErrorResponse => s ! ErrorResponse(req, x.ex)
      }})

    case SetProfileAccessor(profileActor) => {
      profileAccessor = profileActor
    }
      /*
    case HttpRequest(GET, Uri.Path("/crash"), _, _, _) =>
      sender ! HttpResponse(entity = "About to throw an exception in the request handling actor, " +
        "which triggers an actor restart")
      sys.error("BOOM!")

    case HttpRequest(GET, Uri.Path(path), _, _, _) if path startsWith "/timeout" =>
      log.info("Dropping request, triggering a timeout")

    case HttpRequest(GET, Uri.Path("/stop"), _, _, _) =>
      sender ! HttpResponse(entity = "Shutting down in 1 second ...")
      sender ! Http.Close
      context.system.scheduler.scheduleOnce(1.second) { context.system.shutdown() }

    case _: HttpRequest => sender ! HttpResponse(status = 404, entity = "Unknown resource!")

    case Timedout(HttpRequest(_, Uri.Path("/timeout/timeout"), _, _, _)) =>
      log.info("Dropping Timeout message")

    case Timedout(HttpRequest(method, uri, _, _, _)) =>
      sender ! HttpResponse(
        status = 500,
        entity = "The " + method + " request to '" + uri + "' has timed out..."
      )
        */
  }

  ////////////// helpers //////////////

  lazy val index = HttpResponse(
    entity = HttpEntity(`text/html`,
      """<html>
        <body>
          <h1>Say hello to <i>spray-can</i>!</h1>
          <p>Defined resources:</p>
          <ul>
            <li><a href="/ping">/ping</a></li>
            <li><a href="/stream">/stream</a></li>
            <li><a href="/server-stats">/server-stats</a></li>
            <li><a href="/crash">/crash</a></li>
            <li><a href="/timeout">/timeout</a></li>
            <li><a href="/timeout/timeout">/timeout/timeout</a></li>
            <li><a href="/stop">/stop</a></li>
          </ul>
          <p>Test file upload</p>
          <form action ="/file-upload" enctype="multipart/form-data" method="post">
            <input type="file" name="datafile" multiple=""></input>
            <br/>
            <input type="submit">Submit</input>
          </form>
        </body>
      </html>"""
    )
  )


}