package iwebdev.client.renderer

import iwebdev.client.api.Init
import iwebdev.model.WebDev
import iwebdev.model.WebDev.Info
import org.scalajs.dom.window
import org.scalajs.dom.ext._
import org.scalajs.dom.{Node, Text, document}
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

    // We only need to update the TextNode ...
    def updateText() = {

      //node.innerHTML = info.content

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
//        Init.run()
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

    if(null != node){
      if(node.getAttribute("hash") != info.hash.toString){
        println("We got a new hash: " + info.hash)
      } else {
        println("the has is the same: " + info.hash)
      }
      document.body.childNodes.foreach { n =>
        if(n.isInstanceOf[Node])
          document.body.removeChild(n)
      }
      updateDomNode()
      createElement()
//      updateText()



//      Init.run()
      node.setAttribute("hash", "hash-" + info.hash.toString)
    }
     else
      installHtmlElement()
  }

}
