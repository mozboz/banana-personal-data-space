/*
package actors.behaviors

import akka.actor.ActorRef

trait RequestProxy extends Requester
                   with RequestResponder
                   with MessageHandler{
  /**
   * Proxies the request to the "to"-Actor and then responds to the original Requester.
   * @param request The request to proxy
   * @param to The target actor's ref
   * @param proxyingActor The proxying actor's ref
   */
  def proxy(request:Request, to:ActorRef, proxyingActor:ActorRef) {
    onResponseOf(request, to, proxyingActor, {
      case x => respond(request, x)
    })
  }
}
*/