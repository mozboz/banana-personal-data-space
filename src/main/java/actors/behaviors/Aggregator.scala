package actors.behaviors

import akka.actor.{Actor, ActorRef}

import scala.collection.mutable

trait Aggregator extends Actor with Requester{

  def notifyOne(request:Request,
                one:ActorRef,
                then: () => Unit,
                error:() => Unit) {
    notifySome(request, List(one), then, error)
  }

  /**
   * Sends the specified request to some actors and waits until all responded.
   * @param request The request
   * @param then The continuation which should be called when all responses arrived
   * @param error Timeout or other error continuation
   */
  def notifySome (request:Request,
                  some:Iterable[ActorRef],
                  then:() => Unit,
                  error:() => Unit) {
    val remaining = new mutable.HashSet[ActorRef]
    some.foreach(a => remaining.add(a))

    if (remaining.size == 0) {
      then()
    } else {
      expectResponse(request, (response, sender, completed) => {
        remaining.remove(sender)

        if (remaining.size == 0) { // @todo: add timeout
          completed()
          then()
        }
      })

      for(childActor <- remaining) {
        childActor ! request
      }
    }
  }

  /**
   * Sends the specified request to the recipient and catch the arriving response
   * @param request The request
   * @param aggregator The aggregator which gets the response and a "completed" function as parameters
   * @param then The continuation that should be executed when the aggregation finished
   * @param error The error continuation
   */
  def aggregateOne(request:Request,
                   one:ActorRef,
                   aggregator:(Response, ActorRef, () => Unit) => Unit,
                   then:() => Unit = () => {},
                   error:() => Unit = () => {}) {

    // Wraps the aggregator in a way that 'done' is called implicitly if not done from
    // within the handler
    val aggregatorWrapper = (response:Response, actor:ActorRef, done:() => Unit) => {
      var alreadyDone = false
      val doneWrapper = () => {
        if (!alreadyDone) {
          done()
          alreadyDone = true
        }
      }
      aggregator(response, actor, doneWrapper)
      doneWrapper()
    }
    aggregateSome(request, List(one), aggregatorWrapper, then, error)
  }

  /**
   * Sends the specified request to some actors and aggregates every response that arrives.
   * @param request The request
   * @param aggregator The aggregator which gets the response and a "completed" function as parameters
   * @param then The continuation that should be executed when the aggregation finished
   * @param error The error continuation
   */
  def aggregateSome(request:Request,
                    some:Iterable[ActorRef],
                    aggregator:(Response, ActorRef, () => Unit) => Unit,
                    then:() => Unit = () => {},
                    error:() => Unit = () => {}) {
    val remaining = new mutable.HashSet[ActorRef]
    some.foreach(a => remaining.add(a))

    if (remaining.size == 0) {
      then.apply()
    } else {
      var cancelled = false

      expectResponse(request, (response, sender, completed) => {
        remaining.remove(sender)

        // Wrap the completed call into a function which sets a cancelled-flag
        val finishedAggregation = () => {
          completed()
          cancelled = true
        }

        aggregator(response, sender, finishedAggregation)

        if (remaining.size == 0 || cancelled) {  // @todo: add timeout
          if (!cancelled) // already called when cancelled
            completed()

          then.apply()
        }
      })

      for(childActor <- remaining) {
        childActor ! request
      }
    }
  }
}
