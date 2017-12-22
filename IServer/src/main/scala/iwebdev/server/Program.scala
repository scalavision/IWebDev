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


    fromCss4sQ <- Stream.eval(async.topic[IO, Info](WebDev.createInit))
    fromNodeJSQ <- Stream.eval(async.unboundedQueue[IO, Info])
    clientStream <- Stream.eval(async.unboundedQueue[IO, String])

    css4sServer = new Css4sServer(fromCss4sQ)
    nodeJSClient = new NodeJSClient(fromCss4sQ, fromNodeJSQ)
    webSocketServer = new WebSocketServer(clientStream, fromNodeJSQ, fromCss4sQ)

    css4sServer = new WebDevServer(cssIn, jsIn)
    nodeJSClient = new NodeJSClient(cssIn, fromNodeJSQ)
    webSocketServer = new WebSocketServer(clientStream, fromNodeJSQ)

    cssProcessor <-  Stream(
      css4sServer.stream,
      nodeJSClient.stream,
      webSocketServer.stream
    ).join(3)

  } yield cssProcessor

}


