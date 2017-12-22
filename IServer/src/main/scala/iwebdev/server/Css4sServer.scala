package iwebdev.server

import java.net.{InetAddress, InetSocketAddress}

import cats.effect.IO
import fs2.async.mutable.Topic
import fs2.io.tcp.serverWithLocalAddress
import fs2.{Chunk, Pipe, Stream, async}
import iwebdev.model.WebDev
import scodec.bits.BitVector
import scodec.stream.toLazyBitVector
import Resources._
import iwebdev.model.WebDev.Info

class Css4sServer(topic: Topic[IO, Info]) {

  val localBindAddress = async.promise[IO, InetSocketAddress].unsafeRunSync()

  val debug: Pipe[IO,Array[Byte], Array[Byte]] = _.evalMap { bb =>
    IO {
      println("received data ...?"); println(bb.toString); bb
    }
  }

  val showStyle: Pipe[IO, WebDev.Info, WebDev.Info] = _.evalMap { bb =>
    IO {
      println("Css4s Style: "); println(bb.content); bb
    }
  }

  val extractBytes: Pipe[IO, Array[Byte], WebDev.Info] = s => InfoDecoder.streamDecoder.decode[IO] {
    toLazyBitVector {
      s.map { bb => BitVector.apply(bb) }
    }
  }

  /*
  var cache: Map[String, Int] = Map()

  val hasChanged: Pipe[IO, WebDev.Info, WebDev.Info] = _.evalMap { s =>
    IO {
      cache.get(s.id).fold({
        println("new stylesheet!")
        cache += s.id -> s.contentHash; s
      }) { old =>
        if(old == s.contentHash) {
          println("style sheet has not changed!")
          WebDev(s.id, old, "")
        }
        else {
          println("stylesheet has changed!")
          cache += s.id -> s.contentHash; s
        }
      }
    }
  }*/


  val css4sIn: Stream[IO, Unit] =
    serverWithLocalAddress[IO](new InetSocketAddress(InetAddress.getByName(null), 9004)).flatMap {
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