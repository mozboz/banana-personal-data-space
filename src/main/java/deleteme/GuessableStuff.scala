package deleteme

/**
 * Created with IntelliJ IDEA.
 * User: james
 * Date: 27/06/14
 * Time: 21:00
 * To change this template use File | Settings | File Templates.
 */


class Answer[T] (value: Guessable[T]) {
    var guesses = List[Guessable[T]]() // ordered list
    def makeGuess(guess: Guessable[T]): GuessResult = {
      guesses :+ guess
      value.compare(guess)
  }
    // def guessed: Boolean = guesses.contains(answer)
}

sealed trait GuessResult { def result: String }

case object HIGHER extends GuessResult { val result = "HIGHER" }

case object EQUAL extends GuessResult { val result = "EQUAL" }

case object LOWER extends GuessResult { val result = "LOWER" }