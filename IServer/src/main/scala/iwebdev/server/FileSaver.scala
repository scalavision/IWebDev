package iwebdev.server

import java.nio.file.Paths

import cats.effect.IO

import fs2.{Chunk, Sink, Stream}
import iwebdev.model.WebDev
import Resources._
import iwebdev.model.WebDev.Info
import fs2.concurrent._
import cats.effect.ConcurrentEffect


class FileSaver(
  infoInQ: Topic[IO, Info],
  styleSheets: Queue[IO, Info],
) {

  val toFile: Sink[IO, Info] = _.flatMap { i =>

    val path = i.outputPath

    Stream.chunk(
      Chunk.bytes(i.content.getBytes())
    ).covary[IO].to(fs2.io.file.writeAll[IO](Paths.get(path), blockingExecutionContext))

  }

  val stream: Stream[IO, Unit] =
    Stream(
      infoInQ.subscribe(100).filter(i => i.`type` != WebDev.INIT && i.`type` != WebDev.SBT_INFO ),
      styleSheets.dequeue
    ).parJoin(2).to(toFile)

}
