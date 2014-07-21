package app


import actors.supervisors.{ContextGroupAccessorActor, ContextGroupOwnerActor, ProfileActor}
import akka.actor.{ActorRef, Props, ActorSystem}
import com.typesafe.config.{Config, ConfigFactory}
import events.{PropagateContextGroupOwner, PropagateProfile}
import requests.{Read, ManageContexts}

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

  /*
  val asker = system.actorOf(Props[TestRequester], "Asker")

  1 to 100 foreach( _ => {
    asker ! "Bla"
  })
  */


  var _profileActor : ActorRef = null
  var _contextGroupOwner : ActorRef = null
  var _contextGroupAccessor : ActorRef = null

  

  _profileActor = system.actorOf(Props[ProfileActor], "ProfileActor")

  _contextGroupOwner = system.actorOf(Props[ContextGroupOwnerActor], "ContextGroupOwner")
  _contextGroupOwner ! PropagateProfile(_profileActor)

  _contextGroupAccessor = system.actorOf(Props[ContextGroupAccessorActor], "ContextGroupAccessor")
  _contextGroupAccessor ! PropagateContextGroupOwner(_contextGroupOwner)


  _contextGroupOwner ! ManageContexts(List("Context1", "Context2", "Context3", "Context4", "Context5", "Context6",
                                           "Context7", "Context8", "Context9", "Context10"))

  0 to 10000 foreach( _ => {
    _contextGroupAccessor ! Read("Value1", "Context1")
    _contextGroupAccessor ! Read("Value1", "Context2")
    _contextGroupAccessor ! Read("Value1", "Context3")
    _contextGroupAccessor ! Read("Value1", "Context4")
    _contextGroupAccessor ! Read("Value1", "Context5")
  })

  0 to 10000 foreach( _ => {
    _contextGroupAccessor ! Read("Value1", "Context6")
    _contextGroupAccessor ! Read("Value1", "Context7")
    _contextGroupAccessor ! Read("Value1", "Context8")
    _contextGroupAccessor ! Read("Value1", "Context9")
    _contextGroupAccessor ! Read("Value1", "Context10")
  })
}