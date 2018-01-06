package iwebdev.server

import cats.effect.IO
import fs2._
import iwebdev.model.WebDev
import Resources._
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
    infoInQ <- Stream.eval(async.topic[IO, Info](WebDev.createInit))
    cssCache <- Stream.eval(async.unboundedQueue[IO, Info])
    fromNodeJSQ <- Stream.eval(async.unboundedQueue[IO, Info])
    webClientStream <- Stream.eval(async.unboundedQueue[IO, String])

    webDevServer = new WebDevServer(infoInQ)
    nodeJSClient = new NodeJSClient(infoInQ, cssCache, fromNodeJSQ)
    webSocketServer = new WebSocketServer(webClientStream, fromNodeJSQ, infoInQ)
    fileSaver = new FileSaver(infoInQ, fromNodeJSQ)

    infoStream <-  Stream(
      webDevServer.stream,
      nodeJSClient.stream,
      webSocketServer.stream,
      fileSaver.stream
    ).join(4)

  } yield infoStream

}


