package iwebdev.server

import java.nio.file.Paths

import cats.effect.IO
import fs2.async.mutable.{Queue, Topic}
import fs2.io.tcp.serverWithLocalAddress
import fs2.{Chunk, Pipe, Sink, Stream, async}
import iwebdev.model.WebDev
import scodec.bits.BitVector
import scodec.stream.toLazyBitVector
import Resources._
import iwebdev.model.WebDev.Info

class FileSaver(
  infoInQ: Topic[IO, Info],
  styleSheets: Queue[IO, Info],
) {

  val toFile: Sink[IO, Info] = _.flatMap { i =>

    val path = i.outputPath

    Stream.chunk(
      Chunk.bytes(i.content.getBytes())
    ).covary[IO].to(fs2.io.file.writeAll[IO](Paths.get(path)))

  }

  val stream: Stream[IO, Unit] =
    Stream(
      infoInQ.subscribe(100).filter(i => i.`type` != WebDev.INIT && i.`type` != WebDev.SBT_INFO ),
      styleSheets.dequeue
    ).join(2).to(toFile)

}
