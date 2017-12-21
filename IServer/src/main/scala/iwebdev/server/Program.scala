package iwebdev.server

import cats.effect.IO
import fs2._
import iwebdev.model.WebDev
import Resources._
import iwebdev.model.WebDev.Info


object Program {

  private def log(prefix: String): Sink[IO, Info] = _.evalMap { s =>
    IO {
      println(s"$prefix " + s)
    }
  }

  def cssProgram: Stream[IO, Unit] = for {

    fromCss4sQ <- Stream.eval(async.boundedQueue[IO, Info](100))
    fromNodeJSQ <- Stream.eval(async.boundedQueue[IO, Info](100))
    clientStream <- Stream.eval(async.boundedQueue[IO, String](100))

    css4sServer = new Css4sServer(fromCss4sQ)
    nodeJSClient = new NodeJSClient(fromCss4sQ, fromNodeJSQ)
    webSocketServer = new WebSocketServer(clientStream, fromNodeJSQ)

    cssProcessor <-  Stream(
      css4sServer.stream,
      nodeJSClient.stream,
      webSocketServer.stream
    ).join(3)

  } yield cssProcessor

}


