package client.css

import client.StyleSheet
import org.scalajs.dom.document
import org.scalajs.dom.raw.HTMLStyleElement


object CssRenderer {


  def createStyleElement(styleSheet: StyleSheet): HTMLStyleElement = {
    val e = document.createElement("style").asInstanceOf[HTMLStyleElement]
    e.`type` = "text/css"
    e.id = styleSheet.id.toString
    e appendChild document.createTextNode(styleSheet.content)
    e
  }

  def installStyle(style: HTMLStyleElement): Unit =
    document.head appendChild style

//
//  class StyleElementRenderer(s: Renderer[String]) extends Renderer[HTMLStyleElement] {
//    override def apply(css: Css) =
//      createStyleElement(s(css))
//  }
//
//  final class StyleSheetInlineJsOps(private val ss: StyleSheet.Inline) extends AnyVal {
//    /** Turns this StyleSheet into a `&lt;style&gt;` and adds it to the document DOM. */
//    def addToDocument()(implicit s: Renderer[HTMLStyleElement], e: Env): Unit =
//      installStyle(ss.render[HTMLStyleElement])
//  }
//
//  final class StyleSheetRegistryJsOps(private val r: StyleSheetRegistry) extends AnyVal {
//    /** Registered StyleSheets are turned into a `&lt;style&gt;` and added to the document DOM. */
//    def addToDocumentOnRegistration()(implicit s: Renderer[String], e: Env): Unit =
//      r.onRegistrationN { ss =>
//        val styleStr = ss.map(_.render[String]).mkString("\n")
//        val style = createStyleElement(styleStr)
//        installStyle(style)
//      }
//  }
//}

}
