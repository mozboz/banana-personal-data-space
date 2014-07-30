package app


import actors.supervisors.{ConfigurationActor, ContextGroupAccessorActor, ContextGroupOwnerActor, ProfileActor}
import akka.actor.{ActorRef, Props, ActorSystem}
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


  // @todo: Create a model of the actors and their possible connections (in terms of exchanging messages)
  //        so that messages can be broadcasted more easily. For instance Startup and Shutdown

  val _configurationActor = system.actorOf(Props[ConfigurationActor], "ConfigurationActor")
  val _profileActor = system.actorOf(Props[ProfileActor], "ProfileActor")

  val _httpActor = system.actorOf(Props[HttpActor], "HttpActor")

  _profileActor ! Startup(_configurationActor)

  val _contextGroupOwner = system.actorOf(Props[ContextGroupOwnerActor], "ContextGroupOwner")
  _contextGroupOwner ! ConnectProfile(_profileActor)
  _contextGroupOwner ! Startup(_configurationActor)

  val _contextGroupAccessor = system.actorOf(Props[ContextGroupAccessorActor], "ContextGroupAccessor")
  _contextGroupOwner ! ManageContexts(List("Context1", "Context2", "Context3"))

  _contextGroupAccessor ! ConnectContextGroupOwner(_contextGroupOwner)
  _contextGroupAccessor ! Startup(_configurationActor)

  _contextGroupAccessor ! Write("Key1", "Value1", "Context1")
  _contextGroupAccessor ! Write("Key2", "Value2", "Context1")

  _contextGroupAccessor ! Write("Key1", "Value1", "Context2")
  var string = "a"
  for(i <- 1 to 12)
    string = string + string

  _contextGroupAccessor ! Write("Key2", string, "Context2")

  _contextGroupAccessor ! Read("Key2", "Context2")

  implicit val timeout = Timeout(1000)
  val future =  _contextGroupAccessor ? Read("Key2", "Context2")
  val result = Await.result(future, timeout.duration).asInstanceOf[ReadResponse]


  IO(Http) ! Http.Bind(_httpActor, interface = "0.0.0.0", port = 8080)

  _httpActor ! SetProfileAccessor(_contextGroupAccessor)

  // @todo: This is important because it persists the index. Maybe the index should be flushed automatically...
  //_contextGroupOwner ! Shutdown()
}