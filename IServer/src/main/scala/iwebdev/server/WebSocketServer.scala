package iwebdev.server

import java.net.InetSocketAddress

import cats.effect.IO
import fs2.async.mutable.{Queue, Topic}
import fs2.{Pipe, Stream}
import prickle._
import scodec.Codec
import scodec.codecs._
import spinoco.fs2.http
import spinoco.fs2.http.{HttpResponse, websocket}
import spinoco.fs2.http.websocket.Frame
import Resources._
import iwebdev.model.WebDev

import scala.concurrent.duration._
import iwebdev.model.WebDev.Info
import spinoco.protocol.http.HttpRequestHeader

class WebSocketServer(
  clientData: Queue[IO, String],
  styleSheets: Queue[IO, Info],
  javascript: Topic[IO, Info]
) {

  implicit val codecString: Codec[String] = utf8

  private def log(prefix: String): Pipe[IO, Info, Info] = _.evalMap { s =>
    IO {
      println(s"$prefix ");s
    }
  }

//  private def logInfo(prefix: String): Pipe[IO, Info, Info] = _.evalMap { s =>
//    IO {
//      println(s"$prefix " + s);s
//    }
//  }

  val requestHandler: Pipe[IO, Frame[String], Frame[String]] =  { in =>
    in.flatMap { fromClient =>
        Stream(
          styleSheets.dequeue,
          javascript.subscribe(100).filter(_.`type` == WebDev.JS)
        ).join(2).through(log("pushed"))
          .flatMap(s => Stream.eval(IO { Frame.Text(Pickle.intoString(s)) }))
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
