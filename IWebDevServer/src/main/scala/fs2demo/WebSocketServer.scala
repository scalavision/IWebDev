package fs2demo


import java.net.{InetSocketAddress}

import cats.effect.IO
import fs2.{Pipe, Stream}
import fs2.async.mutable.Queue
import Resources._
import fs2demo.CssSerializer.StyleSheet
import prickle._
import spinoco.fs2.http
import spinoco.fs2.http.websocket
import spinoco.fs2.http.websocket.Frame
import scala.concurrent.duration._
import scodec.Codec
import scodec.codecs._

class WebSocketServer(
  clientData: Queue[IO, String],
  styleSheets: Queue[IO, StyleSheet],
  js: Queue[IO, String]
) {

  implicit val codecString: Codec[String] = utf8

  private def log(prefix: String): Pipe[IO, StyleSheet, StyleSheet] = _.evalMap { s =>
    IO {
      println(s"$prefix " + s);s
    }
  }

  // TODO: Next, See if we can somehow push both javascript and css from two different queues
  val requestHandler: Pipe[IO, Frame[String], Frame[String]] =  { in =>
    in.flatMap { fromClient =>
        styleSheets.dequeue.through(log("pushed"))
          .flatMap(s => Stream.eval(IO { Frame.Text(Pickle.intoString(s)) }))
//      merge
//            js.dequeue.flatMap(s => Stream.eval(IO {Frame.Text(s)}))
    }
  }

  private val server =
    http.server[IO](new InetSocketAddress("127.0.0.1", 9092)) _

  private def webSocket = websocket.server(
    pipe = requestHandler,
    pingInterval = 1000.seconds,
    handshakeTimeout = 10.seconds
  ) _

  def stream: Stream[IO, Unit] = server(webSocket)

}
