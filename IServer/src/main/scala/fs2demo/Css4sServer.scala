package fs2demo

import java.net.{InetAddress, InetSocketAddress}

import cats.effect.IO
import fs2.{Chunk, Pipe, Stream, async}
import fs2.async.mutable.{Queue, Topic}
import fs2.io.tcp.serverWithLocalAddress
import Resources._
import fs2demo.CssSerializer.StyleSheet
import scodec.bits.BitVector
import scodec.stream.toLazyBitVector

class Css4sServer(topic: Topic[IO, StyleSheet]) {

  val localBindAddress = async.promise[IO, InetSocketAddress].unsafeRunSync()

  val debug: Pipe[IO,Array[Byte], Array[Byte]] = _.evalMap { bb =>
    IO {
      println("received data ...?"); println(bb.toString); bb
    }
  }

  val showStyle: Pipe[IO, StyleSheet, StyleSheet] = _.evalMap { bb =>
    IO {
      println("Css4s Style: "); println(bb.content); bb
    }
  }

  val extractBytes: Pipe[IO, Array[Byte], StyleSheet] = s => CssDecoder.streamDecoder.decode[IO] {
    toLazyBitVector {
      s.map { bb => BitVector.apply(bb) }
    }
  }

  var cache: Map[String, Int] = Map()

  val hasChanged: Pipe[IO, StyleSheet, StyleSheet] = _.evalMap { s =>
    IO {
      cache.get(s.id).fold({
        println("new stylesheet!")
        cache += s.id -> s.contentHash; s
      }) { old =>
        if(old == s.contentHash) {
          println("style sheet has not changed!")
          StyleSheet(s.id, old, "")
        }
        else {
          println("stylesheet has changed!")
          cache += s.id -> s.contentHash; s
        }
      }
    }
  }


  val css4sIn: Stream[IO, Unit] =
    serverWithLocalAddress[IO](new InetSocketAddress(InetAddress.getByName(null), 6000)).flatMap {
      case Left(local) =>
        println("binding .." + local)
        Stream.eval_(localBindAddress.complete(local))
      case Right(socketHandle) =>
        socketHandle.map { socket =>
          //This is probably only used in testing or if you want to turn off the server??? .onFinalize(socket.endOfOutput)

          // through(hasChanged).filter(_.content.isEmpty) // used as a simple cache not to process already existing stylesheets ...

          socket.reads(1024).chunks.map(_.toArray).through(extractBytes).to(topic.publish) ++
            Stream.chunk(Chunk.bytes("Received StyleSheet".getBytes)).covary[IO].to(socket.writes()).drain.onFinalize(socket.endOfOutput)
        }
    }.joinUnbounded

}
