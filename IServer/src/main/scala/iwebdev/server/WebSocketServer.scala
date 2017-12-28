package iwebdev.server

import java.net.InetSocketAddress

import cats.effect.IO
import fs2.async.mutable.{Queue, Topic}
import fs2.{Pipe, Stream}
import prickle._
import scodec.Codec
import scodec.codecs._
import spinoco.fs2.http
import spinoco.fs2.http.websocket
import spinoco.fs2.http.websocket.Frame
import Resources._
import iwebdev.model.WebDev

import scala.concurrent.duration._
import iwebdev.model.WebDev.Info

/**
  * Awaiting client weboscket connection on 9092
  *
  * @param clientData Queueing up data from the web client
  * @param styleSheets Info of CSS type
  * @param infoInQ All Incoming Info
  */

class WebSocketServer(
  clientData: Queue[IO, String],
  styleSheets: Queue[IO, Info],
  infoInQ: Topic[IO, Info]
) {

  implicit val codecString: Codec[String] = utf8

  // We join two streams, the styleSheets and javascript topics
  // These are pushed to the client, using Pickle for serialization
  // In order to handle websocket ping / pong, the frames probably
  // needs to be binary
  // The message from the client is enqueued in clientData Queue, not used for anythying yet ...
  val requestHandler: Pipe[IO, Frame[String], Frame[String]] =  { in =>
    in.flatMap { fromClient =>

      Stream.eval(IO {
        fromClient.a
      } ).to(clientData.enqueue).drain ++
        Stream(
          styleSheets.dequeue,
          infoInQ.subscribe(100).filter(_.`type` == WebDev.JS)
        ).join(2).flatMap { s =>
          Stream.eval(IO { Frame.Text(Pickle.intoString(s)) })
        }

    }
  }

  private val server =
    http.server[IO](new InetSocketAddress("127.0.0.1", 9092)) _

  private def webSocket = websocket.server(
    pipe = requestHandler,
    pingInterval = 10000.seconds,
    handshakeTimeout = 10.seconds
  ) _

  def stream: Stream[IO, Unit] = server(webSocket)

}
