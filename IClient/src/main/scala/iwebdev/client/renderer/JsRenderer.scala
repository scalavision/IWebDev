package iwebdev.client.renderer

import iwebdev.model.WebDev
import org.scalajs.dom.ext._
import org.scalajs.dom.{Text, document}
import org.scalajs.dom.raw.HTMLScriptElement

object JsRenderer {

  def createStyleElement(javascript: WebDev.Js): HTMLScriptElement = {
    println("creating script node ..")
    val e = document.createElement("script").asInstanceOf[HTMLScriptElement]
    e.`type` = "text/javascript"
    e.id = javascript.id.toString
    e appendChild document.createTextNode(javascript.content)
    e
  }

  def installJavascript(script: HTMLScriptElement): Unit =
    document.head appendChild script

  def render(j: WebDev.Js): Unit = {

    val node = document.getElementById(j.id)

    if(null != node) {

      val script = node.asInstanceOf[HTMLScriptElement]

      script.childNodes.toIterable.filter(_.isInstanceOf[Text]).foreach{ t =>
        script.removeChild(t)
      }

      script.appendChild(
        document.createTextNode(j.content)
      )

    } else
      installJavascript(createStyleElement(j))

  }

}
