package app


import actors.supervisors._
import actors.workers.TestWorker
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

  val _configurationActor = system.actorOf(Props[ConfigurationActor], "ConfigurationActor")

  val s0 = system.actorOf(Props[TestSupervisor], "s0")

  val s1 = system.actorOf(Props[TestSupervisor], "s1")
  val s2 = system.actorOf(Props[TestSupervisor], "s2")
  val s3 = system.actorOf(Props[TestSupervisor], "s3")

  val w1 = system.actorOf(Props[TestWorker], "w1")
  val w2 = system.actorOf(Props[TestWorker], "w2")
  val w3 = system.actorOf(Props[TestWorker], "w3")

  val w4 = system.actorOf(Props[TestWorker], "w4")
  val w5 = system.actorOf(Props[TestWorker], "w5")
  val w6 = system.actorOf(Props[TestWorker], "w6")

  val w7 = system.actorOf(Props[TestWorker], "w7")
  val w8 = system.actorOf(Props[TestWorker], "w8")
  val w9 = system.actorOf(Props[TestWorker], "w9")

  // This builds a logical actor-tree in contrast
  // to the implicit supervision hierarchy which
  // is only concerned about the creation tree.
  // @todo: Check implications for error-handling, this seems not to be right... Maybe another solution must be used.
  // -> which could be traits which encapsulate
  //    child actor creation etc..
  s0 ! AddConfigurable(List(s1,s2,s3))

  s1 ! AddConfigurable(List(w1,w2,w3))
  s2 ! AddConfigurable(List(w4,w5,w6))
  s3 ! AddConfigurable(List(w7,w8,w9))

  s0 ! Startup(_configurationActor) // Sends Startup recursively to all actors. Returns only when all actors are started.
  s0 ! Shutdown() // Sends Shutdown recursively to all actors. Returns only when all actors are shut down.



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