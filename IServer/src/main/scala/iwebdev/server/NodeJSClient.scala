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

/**
  * This sends all Info CSS topics to node server that postprocesses the CSS
  *
  * It was really hard to make this work, and in reality I had to create
  * a CSS cache to temporary store the Info object, while we send the css
  * to the node js for post processing,
  * The result is zipped with the CSS cache and enqueued to the output Queue
  *
  * We want to keep all the Info properties, only replacing the new postprocessed
  * css content.
  *
  * TODO: The way the content is serialized could probably be very much optimized ...
  *
  * @param in Input Stream of Info objects, we filter on nonEmpty to avoid initial topic, and on Css type only
  * @param cssCache Caching the Info object, while post processing the css
  * @param out
  */

class NodeJSClient (in: Topic[IO, Info], cssCache: Queue[IO, Info], out: Queue[IO, Info]) {

  val localBindAddress =
    async.promise[IO, InetSocketAddress].unsafeRunSync()

  val log: Sink[IO, Info] = _.evalMap { s =>
    IO {
      println("postProcessed: ")
    }
  }

  val stream: Stream[IO, Unit] =
    tcp.client[IO](new InetSocketAddress("127.0.0.1", 5000)).flatMap { socket =>

      // We create a Stream of all the socket side effects, and caching of the Info object
      Stream(
        // reading from the topic, filtering out the initial topic, and using only the CSS `type`
        in.subscribe(100).filter(i => i.content.nonEmpty && i.`type` == WebDev.CSS).flatMap { s =>
          println(s"handling: $s")
          Stream.chunk(Chunk.bytes(s.content.getBytes()))
        }.to(socket.writes()),

        //        in.subscribe(100).filter(i => i.content.nonEmpty && i.`type` == WebDev.CSS).flatMap { s =>
//          Stream.segment(Segment(s.content))
//        }.flatMap{ b =>
//          //TODO: This way of transformation and serialization could probably be done much easier and more efficiently
//          Stream.chunk(ByteVectorChunk(ByteVector.apply(b.getBytes)))
//        }.to(socket.writes()),

          // caching the Info Object, had to do it this way
          // TODO: is there a better way to do this ???
          in.subscribe(100).filter(i => i.content.nonEmpty && i.`type` == WebDev.CSS).to(cssCache.enqueue),

       socket.reads(1024, None)
         // The received css from node is separated by `>>>`, we split the chunks here ...
        .through(text.utf8Decode andThen CssSerializer.splitCssChunks)
        .zip(
          cssCache.dequeue
        ).flatMap { t =>

        val postProcessedSheet = t._1
        val oldSheet = t._2

        Stream.eval( IO {

          oldSheet.copy(
            content = postProcessedSheet
          )

        }).to(out.enqueue).drain

        }.to(log).drain

      ).join(3)

    }

//  val stream: Stream[IO, Unit] =
//    tcp.client[IO](new InetSocketAddress("127.0.0.1", 5000)).flatMap { socket =>
//
//      in.subscribe(100).filter(i => i.content.nonEmpty && i.`type` == WebDev.CSS).flatMap { s =>
//        cssSig.set(s)
//        Stream.segment(Segment(s.content))
//      }.flatMap{ b =>
//
//        Stream.chunk(ByteVectorChunk(ByteVector.apply(b.getBytes)))
//
//      }.to(socket.writes()) merge socket.reads(1024, None)
//        .through(text.utf8Decode andThen CssSerializer.splitCssChunks)
//        .zip(
//          in.subscribe(100)
//          //cssSig.continuous.filter(i => i.content.nonEmpty && i.`type` == WebDev.CSS)
//      ).flatMap { t =>
//
//        val postProcessedSheet = t._1
//        val oldSheet = t._2
//
//        Stream.eval( IO {
//
//          oldSheet.copy(
//            content = postProcessedSheet
//          )
//
//        }).to(out.enqueue).drain
//
//      }.to(log).drain
//    }
}
