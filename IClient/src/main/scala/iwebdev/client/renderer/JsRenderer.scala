package iwebdev.client.renderer

import iwebdev.model.WebDev
import org.scalajs.dom.document
import org.scalajs.dom.raw.{HTMLScriptElement}

object JsRenderer {

  def createStyleElement(javascript: WebDev.Js): HTMLScriptElement = {
    val e = document.createElement("script").asInstanceOf[HTMLScriptElement]
    e.`type` = "text/javascript"
    e.id = javascript.id.toString
    e appendChild document.createTextNode(javascript.content)
    e
  }

  def installJavascript(script: HTMLScriptElement): Unit =
    document.head appendChild script

  def render(j: WebDev.Js): Unit = {
    NodeCleaner(j.id)
    installJavascript(createStyleElement(j))
  }

}
