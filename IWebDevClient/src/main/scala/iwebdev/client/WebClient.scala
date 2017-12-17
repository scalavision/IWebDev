package client

import org.scalajs.dom
import org.scalajs.dom.{Blob, Event, MessageEvent, WebSocket}
import prickle.Unpickle

object WebClient {
  def main(args: Array[String]): Unit = {
    val webSocketClient = new WebSocketClient()
    webSocketClient.run()
  }
}
