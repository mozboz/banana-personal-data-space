package app


import actors.supervisors.{ConfigurationActor, ContextGroupAccessorActor, ContextGroupOwnerActor, ProfileActor}
import akka.actor.{Props, ActorSystem}
import com.typesafe.config.{Config, ConfigFactory}
import events.{ConnectContextGroupOwner, ConnectProfile}
import requests._


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
  _contextGroupAccessor ! Write("Key3", "Value3", "Context1")
  _contextGroupAccessor ! Write("Key4", "Value4", "Context1")

  _contextGroupAccessor ! Write("Key1", "Value1", "Context2")
  _contextGroupAccessor ! Write("Key2", "Value2", "Context2")
  _contextGroupAccessor ! Write("Key3", "Value3", "Context2")


  0 to 10 foreach( _ => {
    _contextGroupAccessor ! Read("Key1", "Context1")
    _contextGroupAccessor ! Read("Key2", "Context1")
    _contextGroupAccessor ! Read("Key3", "Context1")
    _contextGroupAccessor ! Read("Key4", "Context1")
    _contextGroupAccessor ! Read("Key1", "Context2")
    _contextGroupAccessor ! Read("Key2", "Context2")
    _contextGroupAccessor ! Read("Key3", "Context2")
  })

  _contextGroupOwner ! Shutdown()

}