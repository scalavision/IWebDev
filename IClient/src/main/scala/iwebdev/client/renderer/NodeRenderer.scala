package iwebdev.client.renderer

import iwebdev.model.WebDev
import iwebdev.model.WebDev.Info
import org.scalajs.dom.ext._
import org.scalajs.dom.{Text, document}
import org.scalajs.dom.raw.HTMLScriptElement
import org.scalajs.dom.raw.{HTMLElement, Node}

/**
  * Initializing, and updating the inline Javascript and Css content
  */
object NodeRenderer {

  def apply(info: Info): Unit  = {

    val node = document.getElementById(info.id)

    def updateDomNode() = {

      val pNode = node.parentNode

      pNode.childNodes.toIterable.foreach { n =>
        if(n.isInstanceOf[HTMLElement] && n.asInstanceOf[HTMLElement].id == info.id){
          pNode.removeChild(n)
        }
      }

    }

    // We only need to update the TextNode
    // We can not use the innerHtml function, is is not reliable
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
      htmlElement.setAttribute("hash", "hash-" + info.hash.toString)
      htmlElement appendChild document.createTextNode(info.content)
      document.head appendChild htmlElement
    }

    def installHtmlElement() = WebDev(info) match {
      case j: WebDev.Js =>
        println("replacing javascript for dom node with Id: " + j.id + " located at: " + j.outputPath)
        createElement()
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

    def updateHtmlElement() = WebDev(info) match {

      case j: WebDev.Js =>

        // We need to remove old javascript dom, could try to find faster
        // more unsafe ways of doing this (with respect to memleaks)
        document.body.childNodes.foreach { n =>
          if(n.isInstanceOf[Node] && !n.isInstanceOf[HTMLScriptElement])
            document.body.removeChild(n)
        }

        updateDomNode()
        createElement()
        node.setAttribute("hash", "hash-" + info.hash.toString)
        println("replacing javascript with dom node Id: " + j.id + " located at: " + j.outputPath)

      case c: WebDev.Css =>
        println("replacing css with dom node Id: " + c.id + " located at: " + c.outputPath)
        updateText()

      case s: WebDev.SBTInfo =>
        //TODO: create a nice toaster maybe ???
        println("SBT_INFO: " + s.content)

      case i: WebDev.Init.type =>
        println("got an init object from the server, this should not occur, but doesn't do any harm either ...")

      case _ =>
        throw new Exception("ERROR: something was utterly wrong ...")
    }

    if(null != node)
      updateHtmlElement()
     else
      installHtmlElement()
  }

}
