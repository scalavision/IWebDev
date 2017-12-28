package iwebdev.client.api

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

/**
  * Global Interface for the Web client application to implement
  *
  * Whenever there is a new javascript installed into the dom, the run method will
  * be called on the Web client application.
  *
  * TODO: enhance with other things like initial data setup, caching the url in use etc.
  */

@js.native
@JSGlobal
object Init extends js.Object {
  def run(): Unit = js.native
}
