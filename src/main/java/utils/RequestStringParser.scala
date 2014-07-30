package utils

import actors.behaviors.Request
import scala.util.matching.Regex
import requests.Read

object RequestStringParser {
  def parseUrl(requestString: String) = parse(requestString.stripPrefix("/"))
  def parse(requestString: String) = {
    val keyOnly = "([^\\s\\\\]+)".r
    val keyAndContext = "([^\\s\\\\]+)\\/([)^\\s\\\\]+)".r

    requestString match {
      case keyAndContext(key, context) => Read(key, context)
      case keyOnly(context) => Read(null, context)

      case _ => throw new IllegalArgumentException("Poorly formatted Request String")
    }


  }
}
