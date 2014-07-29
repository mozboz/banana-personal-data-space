import org.scalatest._

class RequestStringParser extends FlatSpec with Matchers {

    "a request string" should "parse into correct components" in {

      "/foo".stripPrefix("/") should be ("foo")
    }

    it should "do some other stuff" in {
      "foo" + "bar" should be ("foobar")
    }

}
