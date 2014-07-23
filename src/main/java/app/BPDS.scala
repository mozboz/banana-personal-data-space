package app


import actors.supervisors.{ContextGroupAccessorActor, ContextGroupOwnerActor, ProfileActor}
import akka.actor.{ActorRef, Props, ActorSystem}
import com.typesafe.config.{Config, ConfigFactory}
import events.{DisconnectContextGroupOwner, ConnectContextGroupOwner, ConnectProfile}
import requests.{Write, Read, ManageContexts}


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


  var _profileActor : ActorRef = null
  var _contextGroupOwner : ActorRef = null
  var _contextGroupAccessor : ActorRef = null

  

  _profileActor = system.actorOf(Props[ProfileActor], "ProfileActor")

  _contextGroupOwner = system.actorOf(Props[ContextGroupOwnerActor], "ContextGroupOwner")
  _contextGroupOwner ! ConnectProfile(_profileActor)

  _contextGroupAccessor = system.actorOf(Props[ContextGroupAccessorActor], "ContextGroupAccessor")

  _contextGroupOwner ! ManageContexts(List("Context1", "Context2", "Context3"))


  _contextGroupAccessor ! ConnectContextGroupOwner(_contextGroupOwner)
  _contextGroupAccessor ! DisconnectContextGroupOwner()


  _contextGroupAccessor ! ConnectContextGroupOwner(_contextGroupOwner)


  _contextGroupAccessor ! Read("Key1", "Context1")

  _contextGroupAccessor ! Write("Key1", "Value1", "Context1")
  _contextGroupAccessor ! Write("Key2", "Value2", "Context1")
  _contextGroupAccessor ! Write("Key3", "Value3", "Context1")
  _contextGroupAccessor ! Write("Key4", "Value4", "Context1")


  0 to 10 foreach( _ => {
    _contextGroupAccessor ! Read("Key1", "Context1")
    _contextGroupAccessor ! Read("Key2", "Context1")
    _contextGroupAccessor ! Read("Key3", "Context1")
    _contextGroupAccessor ! Read("Key4", "Context1")
  })
}