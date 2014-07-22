package requests

import java.net.URL

import actors.behaviors.Request

case class AggregateRequest(contextUrl:URL) extends Request
