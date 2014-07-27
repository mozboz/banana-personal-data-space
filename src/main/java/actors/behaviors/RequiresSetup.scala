package actors.behaviors

import requests.{SetupResponse, Setup}

/**
 * Handles the Setup-request.
 */
trait RequiresSetup extends RequestResponder {
  def handleSetup(x:Setup) {
    doSetup(x)
    respond(x, new SetupResponse(x))
  }

  def doSetup(x:Setup)
}