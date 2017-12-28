package iwebdev.client
import iwebdev.client.api.Init

object WebClient {

  def main(args: Array[String]): Unit = {
    val ws = new WebSocketClient()
    ws.run()
    println("running application ..")
    //Init.run()
  }

}
