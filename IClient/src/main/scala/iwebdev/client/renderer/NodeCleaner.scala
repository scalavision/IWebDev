package iwebdev.client.renderer

import org.scalajs.dom
import org.scalajs.dom.ext._
import org.scalajs.dom.raw.{HTMLElement, Node}

object NodeCleaner {

  def apply(nodeId: String): Unit = {

    val node = dom.document.getElementById(nodeId)

    if(null != node) {

      val pNode: Node = node.parentNode
      pNode.childNodes.toIterable.foreach { n =>
        if(n.isInstanceOf[HTMLElement] && n.asInstanceOf[HTMLElement].id == nodeId){
          pNode.removeChild(n)
        }
      }

    }

  }

}
