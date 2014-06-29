import collection.mutable.Stack
import model.Profile
import org.scalatest._

class modelTest extends FlatSpec with Matchers {

  "a profile" should "add and test existence of context" in {
    val contextName: String = "personal-info"
    val contextDoesntExist: String = "non existant profile"
    val p: Profile = new Profile("http://james")
    p.createContext(contextName)
    assert(p.contextExists(contextName))
    assert(!p.contextExists(contextDoesntExist))
  }

  // it should ""

}