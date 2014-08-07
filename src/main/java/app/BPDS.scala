package app


import actors.behaviors.Response
import actors.supervisors._
import akka.actor.{Props, ActorSystem}
import com.typesafe.config.{Config, ConfigFactory}
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
  implicit val timeout = Timeout(100)

  // Create the configuration actor
  val _configurationActor = system.actorOf(Props[Configuration], "Configuration")

  // Create the profile actor (which is the root of all following actors)
  val _profileActor = system.actorOf(Props[Profile], "Profile")

  // The config actor is abused as a registry at the moment, so send the running root-actor
  // to the config, so that it can be discovered by other actors who have access to the config.
  val f1 =  _configurationActor ? WriteConfig("profileActor", _profileActor)
  val r1 = Await.result(f1, timeout.duration).asInstanceOf[Response]

  r1 match {
    // When the profile actor was propagated to the configuration ...
    case x:WriteConfigResponse => {

      // Create the HTTP-Endpoint for the profile
      val f2 = _profileActor ? Spawn(Props[HttpActor], "HttpActor")
      val r2 = Await.result(f2, timeout.duration).asInstanceOf[Response]

      r2 match {
        // When the HTTP-endpoint was created successfully, bind it to a ip and port.
        case x:SpawnResponse => IO(Http) ! Http.Bind(x.actorRef, interface = "0.0.0.0", port = 8080)
      }

      // Start the profile system
      val f3 =  _profileActor ? Start(_configurationActor)
      val r3 = Await.result(f3, timeout.duration).asInstanceOf[Response]

      r3 match {
        // If a StartResponse arrives, the profile was started successfully and can be used
        case x:StartResponse => {

          _profileActor ! Write("Key1", "Value1", "Context1")
          _profileActor ! Write("Key2", "Value2 - modified", "Context1")
          _profileActor ! Write("Key3", "Value3", "Context1")
          _profileActor ! Write("Key4", "Value4", "Context1")
          _profileActor ! Write("Key1", "Value1", "Context2")
          _profileActor ! Read("Key1", "Context1")

          // Stops the profile system (flush all caches etc..)
          //_profileActor ! Stop(_profileActor)
        }
      }
    }
  }
}