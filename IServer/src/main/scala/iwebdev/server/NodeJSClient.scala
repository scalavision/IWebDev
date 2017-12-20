package iwebdev.server

import java.net.InetSocketAddress

import cats.effect.IO
import fs2.async.mutable.{Queue, Topic}
import fs2.interop.scodec.ByteVectorChunk
import fs2.io.tcp
import fs2.{Chunk, Segment, Sink, Stream, async, text}
import scodec.bits.ByteVector
import iwebdev.model.WebDev
import iwebdev.model.WebDev.Info
import Resources._

class NodeJSClient (in: Topic[IO, Info], out: Queue[IO, Info]) {

  val localBindAddress =
    async.promise[IO, InetSocketAddress].unsafeRunSync()

  val logInfo: Sink[IO, Info] = _.evalMap { s =>
    IO {
      println("info : " + s)
    }
  }

  val log: Sink[IO, String] = _.evalMap { s =>
    IO {
      println("css : " + s)
    }
  }

  // Keep for testing purposes ...
  // val message = Chunk.bytes("a { display: flex; }".getBytes)
  // Stream.chunk(message).covary[IO].to(socket.writes()).drain.onFinalize(socket.endOfOutput)

  def client(oldInfo: Info, in: Queue[IO, Info], out: Queue[IO, Info]): Stream[IO, Unit] =
    tcp.client[IO](new InetSocketAddress("127.0.0.1", 5000)).flatMap { socket =>

      in.dequeue.map{ _.content }.filter(_.nonEmpty).flatMap { s => Stream.eval(socket.write( Chunk.bytes(s.getBytes()) )) } merge
        socket.reads(1024).through(text.utf8Decode andThen CssSerializer.splitCssChunks).map{ css =>
          Info(
            oldInfo.id,
            oldInfo.id,
            css.hashCode(),
            oldInfo.outputPath,
            css
          )
        }.to(out.enqueue).drain

    }

  val stream2: Stream[IO, Unit] =
    for {
      nodeJSData <- Stream.eval(async.boundedQueue[IO, Info](100))
      _ = in.subscribe(10).to(nodeJSData.enqueue).drain
      oldInfo <- in.subscribe(10)
      program <- client(oldInfo, nodeJSData, out).drain
    } yield program

  val stream3: Stream[IO, Unit] =
    tcp.client[IO](new InetSocketAddress("127.0.0.1", 5000)).flatMap { socket =>

        println("data received ???")

        for {
          nodeJSQ <- Stream.eval(async.boundedQueue[IO, String](100))
          oldInfo <- Stream.eval(async.boundedQueue[IO, Info](100))
//          a <- in.subscribe(10).map(_.content).filter(_.nonEmpty).observe(nodeJSQ.enqueue).to(log)
          b <- in.subscribe(10).filter(_.content.nonEmpty).observe(oldInfo.enqueue).to(logInfo)
          nodeJS <- in.subscribe(10).filter(_.content.nonEmpty).flatMap(s => Stream.segment(Segment(s.content))).flatMap { b => Stream.chunk(ByteVectorChunk(ByteVector.apply(b.getBytes)))}.to(socket.writes())
          cssProcessed <- socket.reads(1024, None).through(text.utf8Decode andThen CssSerializer.splitCssChunks).observe(log).to(nodeJSQ.enqueue)
          both <- nodeJSQ.dequeue.zip(oldInfo.dequeue).flatMap { t =>
            println("got some returned stuff ...")
            val postProcessedSheet = t._1
            val oldSheet = t._2
            Stream.eval(
              IO {
                WebDev.createInfo(
                  oldSheet.id,
                  oldSheet.outputPath,
                  postProcessedSheet,
                  WebDev.CSS
                )
              }
            )
          }
          program <- Stream(nodeJS, cssProcessed, both).covary[IO]
        } yield program

    }

  val stream: Stream[IO, Unit] =
    tcp.client[IO](new InetSocketAddress("127.0.0.1", 5000)).flatMap { socket =>
      in.subscribe(100).filter(i => i.content.nonEmpty && i.`type` == WebDev.CSS).flatMap { s =>

        Stream.segment(Segment(s.content))

      }.flatMap { b =>

        Stream.chunk(ByteVectorChunk(ByteVector.apply(b.getBytes)))

      }.to(socket.writes()) merge socket.reads(1024, None)
        .through(text.utf8Decode andThen CssSerializer.splitCssChunks)
        .zip(
          in.subscribe(10).filter(_.content.nonEmpty)
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
