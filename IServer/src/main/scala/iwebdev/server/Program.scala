package iwebdev.server

import cats.effect.IO
import fs2._
import fs2.concurrent._
import iwebdev.model.WebDev
import Resources._

import fs2.concurrent.Topic
import iwebdev.model.WebDev.Info

/**
  * The main program running the Instant WebDev DevOps server
  *
  * It contains these parts:
  *
  * - WebDevServer:
  *     - Receives css and javascript via the [[Info]] interface
  *     - infoInQ handles a stream [[Info]] of CSS type as a topic
  * - nodeJSClient:
  *     - transforms the infoInQ from raw css to postprocessed css via a Node server
  *     - the old css in infoInQ is replaced by the postprocessed css
  *     - the new css with old [[Info]] properties is written to fromNodeJSQ Queue
  * - webSocketServer:
  *     - handles websocket connection with the web client (browser)
  *     - pushes css results (fromNodeJSQ Queue) and javascript Info objects (filtered from infoInQ)
  *
  * Having all submodules (services) running with a Stream[IO, Unit] was really helpful
  * in order to use the types to drive an aligned set of streams. This is all `wip` and
  * will be adjusted when learning more about fs2.
  *
  * All in all, this works satisfactory, but there are probably lots of ways for improvement!
  *
  */

object Program {

  def processInfoStream: Stream[IO, Unit] = for {
    infoInCssQ <- Stream.eval(Topic[IO, Info](WebDev.createInit))
    infoInJsQ <- Stream.eval(Topic[IO, Info](WebDev.createInit))
    cssCache <- Stream.eval(Queue.unbounded[IO, Info])
//    fromNodeJSQ <- Stream.eval(async.unboundedQueue[IO, Info])
    fromNodeJSQ <- Stream.eval(Queue.unbounded[IO, Info])
    webClientStream <- Stream.eval(Queue.unbounded[IO, String])

    jsDevServer = new JSRawIn(infoInJsQ)
    cssDevServer = new CssRawIn(infoInCssQ)
    nodeJSClient = new NodeJSClient(infoInCssQ, cssCache, fromNodeJSQ)
    webSocketServer = new WebSocketServer(webClientStream, fromNodeJSQ, infoInJsQ)
    fileSaver = new FileSaver(infoInCssQ, fromNodeJSQ)

    infoStream <-  Stream(
      cssDevServer.stream,
      jsDevServer.stream,
      nodeJSClient.stream,
      webSocketServer.stream,
      fileSaver.stream
    ).parJoin(5)

  } yield infoStream

}


