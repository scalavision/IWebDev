package fs2demo

import cats.effect.IO
import fs2demo.CssSerializer.StyleSheet
import Resources._
import fs2._

object Program {

  private def log(prefix: String): Pipe[IO, StyleSheet, StyleSheet] = _.evalMap { s =>
    IO {
      println(s"$prefix " + s);s
    }
  }

  val cssProgram: Stream[IO, Unit] = for {

    fromCss4sQ <- Stream.eval(async.topic[IO, StyleSheet](StyleSheet.create("")))
    fromNodeJSQ <- Stream.eval(async.boundedQueue[IO, StyleSheet](100))
    clientStream <- Stream.eval(async.boundedQueue[IO, String](100))

    css4sServer = new Css4sServer(fromCss4sQ)
    nodeJSClient = new NodeJSClient(fromCss4sQ, fromNodeJSQ)
    webSocketServer = new WebSocketServer(clientStream, fromNodeJSQ)

    cssProcessor <-  Stream(
      css4sServer.css4sIn,
      nodeJSClient.stream,
      webSocketServer.stream
    ).join(3)

  } yield cssProcessor

}


