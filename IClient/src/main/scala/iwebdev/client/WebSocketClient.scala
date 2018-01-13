package iwebdev.client

import iwebdev.client.renderer.NodeRenderer
import iwebdev.client.ws.PingFrame
import iwebdev.model.WebDev.Info
import org.scalajs.dom
import org.scalajs.dom.{Blob, WebSocket}
import prickle.Unpickle
import scala.scalajs.js.timers._
import scala.scalajs.js.annotation.JSExportAll


/**
  * Connection with the WebDev server, getting [[iwebdev.model.WebDev]] Info objects from server.
  * For each Info object there will be a corresponding dom node created from the `content` property.
  * The Info object's `id` property is used as the dom node / elements id. An update removes and
  * adds the dom node again ..
  *
  */

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

    // TODO: implement a working Ping / Pong protocol with the websocket server
//    The WebSocket Ping / Pong is still WIP
//    setInterval(10000) {
//      println("pinging the server")
//      sendPong()
//    }

    socket.onopen = { (e: dom.Event) =>

      println("sending ready signal to server ...")
      sendMessage("READY")

    }

    socket.onmessage = { (e: dom.MessageEvent) =>

      e.data match {
        case s : String =>
          val info = Unpickle[Info].fromString(s).get
          NodeRenderer(info)

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
