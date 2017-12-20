package iwebdev.client.renderer

import iwebdev.model.WebDev
import org.scalajs.dom.document
import org.scalajs.dom.raw.HTMLStyleElement


object CssRenderer {

  def createStyleElement(styleSheet: WebDev.Css): HTMLStyleElement = {
    val e = document.createElement("style").asInstanceOf[HTMLStyleElement]
    e.`type` = "text/css"
    e.id = styleSheet.id.toString
    e appendChild document.createTextNode(styleSheet.content)
    e
  }

  def installStyle(style: HTMLStyleElement): Unit =
    document.head appendChild style

  def render(css: WebDev.Css) = {
    NodeCleaner(css.id)
    installStyle(createStyleElement(css))
  }
}
