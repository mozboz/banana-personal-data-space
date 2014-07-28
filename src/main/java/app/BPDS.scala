package app


import actors.supervisors.{ConfigurationActor, ContextGroupAccessorActor, ContextGroupOwnerActor, ProfileActor}
import akka.actor.{Props, ActorSystem}
import com.typesafe.config.{Config, ConfigFactory}
import events.{ConnectContextGroupOwner, ConnectProfile}
import requests._
import concurrent.Await
import akka.pattern.ask
import akka.util.Timeout

// import akka.util.Timeout
import scala.concurrent.duration._


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

  _profileActor ! Setup(_configurationActor)

  val _contextGroupOwner = system.actorOf(Props[ContextGroupOwnerActor], "ContextGroupOwner")
  _contextGroupOwner ! ConnectProfile(_profileActor)
  _contextGroupOwner ! Setup(_configurationActor)

  val _contextGroupAccessor = system.actorOf(Props[ContextGroupAccessorActor], "ContextGroupAccessor")
  _contextGroupOwner ! ManageContexts(List("Context1", "Context2", "Context3"))

  _contextGroupAccessor ! ConnectContextGroupOwner(_contextGroupOwner)
  _contextGroupAccessor ! Setup(_configurationActor)

  _contextGroupAccessor ! Write("Key1", "Value1", "Context1")
  _contextGroupAccessor ! Write("Key2", "Value2", "Context1")

  _contextGroupAccessor ! Write("Key1", "Value1", "Context2")
  _contextGroupAccessor ! Write("Key2", "Value2", "Context2")

  implicit val timeout = Timeout(5000)
  val future =  _contextGroupAccessor ? Read("Key2", "Context2")
  val result = Await.result(future, timeout.duration).asInstanceOf[ReadResponse]

  println(result)
  println(System.getProperty("foo"))

  _contextGroupOwner ! Shutdown()
}