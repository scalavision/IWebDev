package iwebdev.server

import java.net.InetSocketAddress

import cats.effect.IO
import fs2.async.mutable.{Queue, Topic}
import fs2.interop.scodec.ByteVectorChunk
import fs2.io.tcp
import fs2.{Segment, Sink, Stream, async, text}
import scodec.bits.ByteVector
import iwebdev.model.WebDev
import iwebdev.model.WebDev.Info
import Resources._

class NodeJSClient (in: Topic[IO, Info], out: Queue[IO, Info]) {

  val localBindAddress =
    async.promise[IO, InetSocketAddress].unsafeRunSync()

  val log: Sink[IO, Info] = _.evalMap { s =>
    IO {
      println("postProcessed: ")
    }
  }

  // Keep for testing purposes ...
  // val message = Chunk.bytes("a { display: flex; }".getBytes)
  // Stream.chunk(message).covary[IO].to(socket.writes()).drain.onFinalize(socket.endOfOutput)

  val stream: Stream[IO, Unit] =
    tcp.client[IO](new InetSocketAddress("127.0.0.1", 5000)).flatMap { socket =>

      in.subscribe(100).filter(i => i.content.nonEmpty && i.`type` == WebDev.CSS).flatMap { s =>
        Stream.segment(Segment(s.content))
      }.flatMap{ b =>

        Stream.chunk(ByteVectorChunk(ByteVector.apply(b.getBytes)))

      }.to(socket.writes()) merge socket.reads(1024, None)
        .through(text.utf8Decode andThen CssSerializer.splitCssChunks)
        .zip(
          in.subscribe(100)
      ).flatMap { t =>

        val postProcessedSheet = t._1
        val oldSheet = t._2
        Stream.eval( IO {

          WebDev.createInfo(
            oldSheet.id,
            oldSheet.outputPath,
            postProcessedSheet,
            WebDev.CSS
          )

        }).to(out.enqueue).drain

      }.to(log).drain
    }
}
