package fs2demo

import java.net.{InetSocketAddress}

import cats.effect.IO
import fs2.{Segment, Sink, Stream, async, text}
import fs2.async.mutable.{Queue, Topic}
import Resources._
import fs2.async.Promise
import fs2.interop.scodec.ByteVectorChunk
import fs2.io.tcp
import fs2demo.CssSerializer.StyleSheet
import scodec.bits.{ByteVector}

class NodeJSClient (in: Topic[IO, StyleSheet], out: Queue[IO, StyleSheet]) {

  val localBindAddress =
    async.promise[IO, InetSocketAddress].unsafeRunSync()

  val log: Sink[IO, StyleSheet] = _.evalMap { s =>
    IO {
      println("postProcessed: ")
    }
  }

  // Keep for testing purposes ...
  // val message = Chunk.bytes("a { display: flex; }".getBytes)
  // Stream.chunk(message).covary[IO].to(socket.writes()).drain.onFinalize(socket.endOfOutput)

  val stream: Stream[IO, Unit] =
    tcp.client[IO](new InetSocketAddress("127.0.0.1", 5000)).flatMap { socket =>
      in.subscribe(100).filter(_.content.nonEmpty).flatMap { s =>

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

          StyleSheet(oldSheet.id,
            postProcessedSheet.hashCode,
            postProcessedSheet)
        } ).to(out.enqueue).drain

      }.to(log).drain
    }
}
