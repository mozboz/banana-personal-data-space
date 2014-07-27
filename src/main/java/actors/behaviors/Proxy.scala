
package actors.behaviors

import akka.actor.{Actor, ActorRef}

trait Proxy extends Requester{
  this: Actor =>
  /**
   * Proxies the request to the "to"-Actor and then responds to the original Requester.
   * @param request The request to proxy
   * @param to The target actor's ref
   */
  def proxy(request:Request, to:ActorRef, sender:ActorRef) {
    onResponseOf(request, to, this.self, {
      case x => sender ! x
    })
  }
}