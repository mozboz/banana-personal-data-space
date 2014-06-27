package actors

import akka.actor.Actor

class MasterActor extends Actor {

  def receive = {
    case "START" =>
      // remote ! guess
    case msg: String =>
      println(s"LocalActor received message: '$msg'")

    case msg: Boolean =>
      if (msg) {
        println(s"Local guessed correct")
      } else {
        println(s"Local guessed wrong,  trying next")
        // guess += 1
        // remote ! guess
      }
  }
}

