class Answer[T] (value: Guessable[T]) {
    var guesses = List[Guessable[T]]() // ordered list
    def makeGuess(guess: Guessable[T]): GuessResult = {
      guesses :+ guess
      value.compare(guess)
  }
    // def guessed: Boolean = guesses.contains(answer)
}

class Guessable[T <% Ordered[T]] (var value: T) {
  def compare(guess: Guessable[T]): GuessResult = {
    if (guess.value < value) {
        LOWER
    } else if (guess.value == value) {
        EQUAL
    } else {
        HIGHER
    }
  }
}

sealed trait GuessResult { def result: String }
case object HIGHER extends GuessResult { val result = "HIGHER" }
case object EQUAL extends GuessResult { val result = "EQUAL" }
case object LOWER extends GuessResult { val result = "LOWER" }



