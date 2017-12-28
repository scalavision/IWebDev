package iwebdev.client.renderer

import iwebdev.client.api.Init
import iwebdev.model.WebDev
import iwebdev.model.WebDev.Info
import org.scalajs.dom.ext._
import org.scalajs.dom.{Text, document}
import org.scalajs.dom.raw.HTMLScriptElement

/**
  * Initializing, and updating the inline Javascript and Css content
  */
object NodeRenderer {

  def apply(info: Info): Unit  = {

    val node = document.getElementById(info.id)

    // We only need to update the TextNode ...
    def updateText() = {
      node.childNodes.toIterable.filter(_.isInstanceOf[Text]).foreach{ t =>
        node.removeChild(t)
      }
      node.appendChild(
        document.createTextNode(info.content)
      )
    }

    def createElement() =  {
      val element = if(info.`type` == WebDev.JS) "script" else "style"
      val `type` = if(info.`type` == WebDev.JS) "text/javascript" else "text/css"
      val htmlElement = document.createElement(element).asInstanceOf[HTMLScriptElement]
      htmlElement.`type` = `type`
      htmlElement.id = info.id
      htmlElement appendChild document.createTextNode(info.content)
      document.head appendChild htmlElement
    }

    def installHtmlElement() = WebDev(info) match {
      case j: WebDev.Js =>
        println("replacing javascript for dom node with Id: " + j.id + " located at: " + j.outputPath)
        createElement()
        Init.run()
      case c: WebDev.Css =>
        println("replacing javascript for dom node with Id: " + c.id + " located at: " + c.outputPath)
        createElement()
      case s: WebDev.SBTInfo =>
        //TODO: create a nice toaster maybe ???
        println("SBT_INFO: " + s.content)
      case i: WebDev.Init.type =>
        println("got an init object from the server, this should not occur, but doesn't do any harm either ...")
      case _ =>
        throw new Exception("ERROR: something was utterly wrong ...")
    }

    if(null != node)
      updateText()
     else
      installHtmlElement()
  }

}
