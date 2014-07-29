
package actors.behaviors

import akka.actor.{Actor, ActorRef}
import requests.{ErrorResponse, ReadResponse, ReadFromContext}

trait Proxy extends BaseActor{
  /**
   * Proxies the request to the "to"-Actor and then passes the response to the original Requester.
   * @param request The request to proxy
   * @param to The target actor's ref
   */
  def proxy(request:Request, to:ActorRef, sender:ActorRef) {
    val original = sender
    aggregateOne(request, to, (response, sender, done) => {
      original ! response
    })
    /*
    onResponseOf(request, to, this.self, {
      case x => sender ! x
    })*/
  }
}