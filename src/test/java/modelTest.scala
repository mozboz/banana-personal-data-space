import app.ProfileStorage
import model.Profile
import org.scalatest._

class modelTest extends FlatSpec with Matchers {

  val contextKey = "flib"
  val invalidContextName = "baz"

  val contentKey = "key"
  val contentValue = "value"

  "a profile" should "add and test existence of context" in {
    val p = new Profile("http://james", new ProfileStorage())
    p.createContext(contextKey)
    assert(p.contextExists(contextKey))
    assert(!p.contextExists(invalidContextName))
  }

  "a context" should "add, test existence and retrieve a content item" in {
    val p = new Profile("http://james", new ProfileStorage())
    p.createContext(contextKey)
    p.addContentItem(contextKey, contentKey, contentValue)

    assert(p.contentItemExists(contextKey, contentKey))
    assert(p.getContentItem(contextKey, contentKey) == contentValue)

  }

}