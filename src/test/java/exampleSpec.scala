import collection.mutable.Stack
import local._
import org.scalatest._

class ExampleSpec extends FlatSpec with Matchers {

  trait Guesses {
    var lowGuess = new Guessable[Int](1)
    var mediumGuess = new Guessable[Int](2)
    var anotherMediumGuess = new Guessable[Int](2)
    var highGuess = new Guessable[Int](3)
  }

  "a guessable int" should "compare correctly to other guessable ints" in new Guesses {
      mediumGuess.compare(anotherMediumGuess) should be (local.EQUAL)
  }

  "A Stack" should "pop values in last-in-first-out order" in {
    val stack = new Stack[Int]
    stack.push(1)
    stack.push(2)
    stack.pop() should be (2)
    stack.pop() should be (1)
  }

  it should "throw NoSuchElementException if an empty stack is popped" in {
    val emptyStack = new Stack[Int]
    a [NoSuchElementException] should be thrownBy {
      emptyStack.pop()
    }
  }
}