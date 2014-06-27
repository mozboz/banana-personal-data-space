package deleteme

import deleteme.{GuessResult, LOWER, EQUAL, HIGHER}

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