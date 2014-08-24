import org.scalatest._
import utils.RequestStringParser

class RequestStringParserTest extends FlatSpec with Matchers {

    "a request string" should "parse into correct components" in {

      // "/foo".stripPrefix("/") should be ("foo")
      RequestStringParser.parse("foo/bar") should be ("foobar")
    }

    it should "do some other stuff" in {
      "foo" + "bar" should be ("foobar")
    }

}
