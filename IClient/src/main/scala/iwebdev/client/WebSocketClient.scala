package iwebdev.client

import iwebdev.client.api.Init
import iwebdev.client.renderer.{CssRenderer, JsRenderer}
import iwebdev.client.ws.PingFrame
import iwebdev.model.WebDev
import iwebdev.model.WebDev.{Info, ReplaceInfo}
import org.scalajs.dom
import org.scalajs.dom.raw.{BlobPropertyBag, HTMLStyleElement}
import org.scalajs.dom.{Blob, WebSocket}
import prickle.Unpickle
import scala.scalajs.js.timers._

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
class WebSocketClient {

  val socket = new WebSocket("ws://127.0.0.1:9092")

  def sendPong() = {
    socket.send(PingFrame.pong)
  }

  def run() = {

    println("starting websocket client ...")

    def sendMessage(msg: String) = {
      socket.send(msg)
    }

//    Not working yet ...
//    setInterval(10000) {
//      println("pinging the server")
//      sendPong()
//    }

    socket.onopen = { (e: dom.Event) =>

      println("sending ready signal to server ...")
      sendMessage("READY")

    }

    def replaceNode(info: ReplaceInfo): Unit = info match {
        case j: WebDev.Js =>
          println("js")
          JsRenderer.render(j)
          Init.run()
        case c: WebDev.Css =>
          println("css")
          CssRenderer.render(c)
        case _ =>
          throw new Exception("ERROR: something was utterly wrong ...")
      }

    socket.onmessage = { (e: dom.MessageEvent) =>

      println("received a message")

      e.data match {
        case s : String =>
          val info = Unpickle[Info].fromString(s).get
          println("updateing client ...")
          replaceNode(WebDev(info))

        case b: Blob =>
          b.`type`
          println("undefined result : " + e.data)
          println(b.size)
          println(b.`type`)
          throw new Exception("ERROR OCCURED, RECEIVED BINARY INSTEAD OF STRINGS ON WEBSOCKET !!!!")
      }

    }


  }

}
