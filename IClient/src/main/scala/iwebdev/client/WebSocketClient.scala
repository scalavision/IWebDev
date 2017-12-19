package client

import client.css.CssRenderer
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLStyleElement
import org.scalajs.dom.{Blob, WebSocket}
import prickle.Unpickle
import CssRenderer._

case class StyleSheet(
  id: String,
  contentHash: Int,
  content: String
)

class WebSocketClient {

  val socket = new WebSocket("ws://127.0.0.1:9092")

  def run() = {

    def sendMessage(msg: String) = {
      socket.send(msg)
    }

    socket.onopen = { (e: dom.Event) =>

      println("sending ready signal to server ...")
      sendMessage("READY")

    }

    socket.onmessage = { (e: dom.MessageEvent) =>

      println("received a message")

      e.data match {
        case s : String =>

          val styleSheet = Unpickle[StyleSheet].fromString(s).get

          val node = dom.document.getElementById(styleSheet.id)

          if(null != node) {
            val pNode = node.parentNode
            while(pNode.hasChildNodes()){
              pNode.removeChild(pNode.firstChild)
            }
          }

          installStyle(
            createStyleElement(styleSheet)
          )

        case b: Blob =>
          println("undefined result : " + e.data)
          println(b.size)
          println(b.`type`)
      }

    }


  }

}
