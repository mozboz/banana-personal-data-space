package actors.behaviors

import akka.actor.{Actor, ActorRef}
import requests.ErrorResponse

import scala.collection.mutable

/**
 * Provides functions to aggregate responses from more than one actor.
 */
trait Aggregator extends Actor with Requester{

  /**
   * Can be used to join different execution branches together.
   * @param times How many branches to join
   * @param joined The continuation which is called when all branches were joined
   * @return The join counter function
   */
  def joiner(times:Int, joined:() => Unit) : () => Unit = {
    var remaining = times
    () => {
      remaining = remaining - 1
      if (remaining == 0)
        joined()
    }
  }

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
   * Requests a value from another actor.
   * @param request The request
   * @param targetActor The target actor
   * @param success Success continuation with response as parameter
   * @param error Error continuation with exception as parameter
   * @tparam TResponse The requested response type
   */
  def request[TResponse](request:Request, targetActor:ActorRef, success:(TResponse) => Unit, error:(Exception) => Unit) {
    aggregateOne(request, targetActor, (response, sender) => {
      response match {
        case x:ErrorResponse => error(x.ex)
        case x:TResponse => success(x)
        case _ => error(new Exception("The response was not understood."))
      }
    })
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
                   aggregator:(Response, ActorRef) => Unit,
                   then:() => Unit = () => {},
                   error:() => Unit = () => {}) {

    val aggregatorWrapper = (response:Response, actor:ActorRef, done:() => Unit) => {
      aggregator(response, actor)
      done()
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

    val remainingActors = new mutable.HashSet[ActorRef]
    some.foreach(a => remainingActors.add(a))

    if (remainingActors.size == 0) {
      then.apply()
    } else {
      var cancelled = false

      expectResponse(request, (response, sender, completed) => {
        remainingActors.remove(sender)

        // Wrap the completed call into a function which sets a cancelled-flag
        val finishedAggregation = () => {
          completed()
          cancelled = true
        }

        aggregator(response, sender, finishedAggregation)

        if (remainingActors.size == 0 || cancelled) {  // @todo: add timeout
          if (!cancelled) // already called when cancelled
            completed()

          then.apply()
        }
      })

      for(remainingActor <- remainingActors) {
        remainingActor ! request
      }
    }
  }
}