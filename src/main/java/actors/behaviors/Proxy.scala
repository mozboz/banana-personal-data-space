
package actors.behaviors

import akka.actor.ActorRef

trait Proxy extends WorkerActor{
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
  }
}