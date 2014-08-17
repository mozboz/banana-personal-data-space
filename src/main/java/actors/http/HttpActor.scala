package actors.http

import akka.util.Timeout
import akka.actor._
import spray.can.Http
import spray.http.HttpHeaders.{`Access-Control-Allow-Origin`, `Content-Type`}
import spray.http._
import HttpMethods._
import MediaTypes._
import requests._
import actors.behaviors.WorkerActor
import requests.ErrorResponse

class HttpActor extends WorkerActor {

  //implicit val timeout: Timeout = 1 // for the actor 'asks'
  //import context.dispatcher // ExecutionContext for the futures and scheduler
  var _profile: ActorRef = _

  def start(sender:ActorRef, message:Start, started:() => Unit) {
    readConfig("profileActor",
    value => {
      _profile = value.asInstanceOf[ActorRef]
      started()
    },
    exception => throw exception)
  }

  def stop(sender:ActorRef, message:Stop, stopped:() => Unit) {

  }

  def handleRequest =  {
    // when a new connection comes in we register ourselves as the connection handler
    case _: Http.Connected => sender ! Http.Register(self)

    case HttpRequest(GET, Uri.Path(path), _, _, _) =>
      val s = sender()
      val req = Read(path.stripPrefix("/"), "Context1")

      aggregateOne(req, _profile, (response, sender) => {
        response match {
        case x:ReadResponse => s ! HttpResponse(entity = HttpEntity(`text/html`, x.data),
          headers = List(`Access-Control-Allow-Origin`(AllOrigins)))
        case x:ErrorResponse => s ! HttpResponse(status = 500, entity = x.ex.getMessage)
      }})
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
}