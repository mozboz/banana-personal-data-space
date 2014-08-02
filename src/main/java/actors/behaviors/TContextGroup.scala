package actors.behaviors

import akka.actor.ActorRef
import utils.ResourceManager

trait TContextGroup {

  private val _contexts = new ResourceManager[String, ActorRef](getActor)

  def withContext(contextKey: String,
                  withContext: (ActorRef) => Unit,
                  error: (Exception) => Unit) {
    _contexts
      .get(contextKey)
      .withResource(withContext, error)
  }

  def getActor(key:String,actor:(ActorRef) => Unit, error:(Exception) => Unit)
}
