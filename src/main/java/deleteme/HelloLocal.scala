package deleteme

import akka.actor._

class LocalActor extends Actor {

  // create the remote actor
  val remote = context.actorFor("akka.tcp://HelloRemoteSystem@127.0.0.1:5150/user/RemoteActor")
  var counter = 0
  var guess = 1

  def receive = {
    case "START" =>
      remote ! guess
    case msg: String =>
      println(s"LocalActor received message: '$msg'")

    case msg: Boolean =>
      if (msg) {
        println(s"Local guessed correct")
      } else {
        println(s"Local guessed wrong, " + guess + " trying next")
        guess += 1
        remote ! guess
      }
  }

}

class Guesser {
  var thisGuess: Guessable[Int] = _
  var pastGuesses = List[Guessable[Int]](new Guessable[Int](0))

  def getGuess(result: GuessResult): Int = {
    val r = makeGuess(result)
    pastGuesses :+ new Guessable[Int](r)
    r
  }

  def makeGuess(result: GuessResult): Int = {
    if (pastGuesses.size == 1) {
      50
    } else if (result == LOWER) {
        pastGuesses.last.value - (math.abs(pastGuesses.last.value - pastGuesses(pastGuesses.size - 2).value / 2))
    } else if (result == HIGHER) {
      pastGuesses.last.value + (math.abs(pastGuesses.last.value - pastGuesses(pastGuesses.size - 2).value / 2))
  } else {
      throw new Exception("omg bad things")
    }

  }

}