package app


import actors.behaviors.Response
import actors.supervisors._
import akka.actor.{Props, ActorSystem}
import com.typesafe.config.{Config, ConfigFactory}
import events.{ConnectContextGroupOwner, ConnectProfile}
import requests._
import concurrent.Await
import akka.pattern.ask
import akka.util.Timeout
import actors.http.HttpActor
import spray.can.Http
import akka.io.IO


object BPDS extends App {


  val config: Config = ConfigFactory.parseString("""akka {
         loglevel = "DEBUG"
         actor {
           debug {
             receive = on
             lifecycle = off
           }
         }
       }""").withFallback(ConfigFactory.load())

  implicit val system = ActorSystem("ProfileSystem", config)
  implicit val timeout = Timeout(1000)

  val _configurationActor = system.actorOf(Props[ConfigurationActor], "ConfigurationActor")
  val _profileActor = system.actorOf(Props[ProfileActor], "ProfileActor")

  // Just for testing:
  // Spawn and immediately stop an actor
  val resp = _profileActor ? Spawn[TestSupervisor]()
  Await.result(resp, timeout.duration).asInstanceOf[Response] match {
    case x:SpawnResponse => _profileActor ! Stop(x.actorRef)
    case x:ErrorResponse => throw x.ex
  }

  val _httpActor = system.actorOf(Props[HttpActor], "HttpActor")

  _profileActor ! Start(_configurationActor)

  val _contextGroupOwner = system.actorOf(Props[ContextGroupOwnerActor], "ContextGroupOwner")
  _contextGroupOwner ! ConnectProfile(_profileActor)
  _contextGroupOwner ! Start(_configurationActor)

  val _contextGroupAccessor = system.actorOf(Props[ContextGroupAccessorActor], "ContextGroupAccessor")
  _contextGroupOwner ! ManageContexts(List("Context1", "Context2", "Context3"))

  _contextGroupAccessor ! ConnectContextGroupOwner(_contextGroupOwner)
  _contextGroupAccessor ! Start(_configurationActor)

  _contextGroupAccessor ! Write("Key1", "Value1", "Context1")
  _contextGroupAccessor ! Write("Key2", "Value2", "Context1")

  _contextGroupAccessor ! Write("Key1", "Value1", "Context2")
  var string = "a"
  for(i <- 1 to 12)
    string = string + string

  _contextGroupAccessor ! Write("Key2", string, "Context2")

  _contextGroupAccessor ! Read("Key2", "Context2")

  val future =  _contextGroupAccessor ? Read("Key2", "Context2")
  val result = Await.result(future, timeout.duration).asInstanceOf[ReadResponse]


  IO(Http) ! Http.Bind(_httpActor, interface = "0.0.0.0", port = 8080)

  _httpActor ! SetProfileAccessor(_contextGroupAccessor)

  // @todo: This is important because it persists the index. Maybe the index should be flushed automatically...
  //_contextGroupOwner ! Shutdown()
}