package iwebdev.client

import iwebdev.client.renderer.{CssRenderer, JsRenderer}
import iwebdev.model.WebDev
import iwebdev.model.WebDev.{Info, ReplaceInfo}
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLStyleElement
import org.scalajs.dom.{Blob, WebSocket}
import prickle.Unpickle

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
class WebSocketClient {

  val socket = new WebSocket("ws://127.0.0.1:9092")

  def run() = {

    println("running ...")

    def sendMessage(msg: String) = {
      socket.send(msg)
    }

    socket.onopen = { (e: dom.Event) =>

      println("sending ready signal to server ...")
      sendMessage("READY")

    }

    def replaceNode(info: ReplaceInfo): Unit = info match {
        case j: WebDev.Js =>
          println("js")
          JsRenderer.render(j)
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

          println("we got this: ")
          println(info)

          replaceNode(WebDev(info))

        case b: Blob =>
          println("undefined result : " + e.data)
          println(b.size)
          println(b.`type`)
      }

    }


  }

}
