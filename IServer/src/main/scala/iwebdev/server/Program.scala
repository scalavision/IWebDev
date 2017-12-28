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

    cssInQ <- Stream.eval(async.topic[IO, Info](WebDev.createInit))
    jsInQ <- Stream.eval(async.unboundedQueue[IO, Info])
    fromNodeJSQ <- Stream.eval(async.unboundedQueue[IO, Info])
    clientStream <- Stream.eval(async.unboundedQueue[IO, String])

    css4sServer = new WebDevServer(cssInQ, jsInQ)
    nodeJSClient = new NodeJSClient(cssInQ, fromNodeJSQ)
    webSocketServer = new WebSocketServer(clientStream, fromNodeJSQ, cssInQ)

    cssProcessor <-  Stream(
      css4sServer.stream,
      nodeJSClient.stream,
      webSocketServer.stream
    ).join(3)

  } yield cssProcessor

}


