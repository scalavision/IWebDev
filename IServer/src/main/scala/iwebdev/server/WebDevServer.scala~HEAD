package iwebdev.server

import java.net.{InetAddress, InetSocketAddress}

import cats.effect.IO
import fs2.async.mutable.{Queue, Topic}
import fs2.io.tcp.serverWithLocalAddress
import fs2.{Chunk, Pipe, Sink, Stream, async}
import iwebdev.model.WebDev
import scodec.bits.BitVector
import scodec.stream.toLazyBitVector
import Resources._
import iwebdev.model.WebDev.Info

// TODO: Create a separate server for javascript
class WebDevServer(cssIn: Topic[IO, Info], jsIn: Queue[IO, Info]) {

  val port = 6000
  val localBindAddress = async.promise[IO, InetSocketAddress].unsafeRunSync()

//  val debug: Pipe[IO,Array[Byte], Array[Byte]] = _.evalMap { bb =>
//    IO {
//      println("received data ...?"); println(bb.toString); bb
//    }
//  }
//
//  val showStyle: Pipe[IO, WebDev.Info, WebDev.Info] = _.evalMap { bb =>
//    IO {
//      println("Css4s Style: "); println(bb.content); bb
//    }
//  }

  val extractBytes: Pipe[IO, Array[Byte], WebDev.Info] = s => InfoDecoder.streamDecoder.decode[IO] {
    toLazyBitVector {
      s.map { bb => BitVector.apply(bb) }
    }
  }

  val splitInputStream: Sink[IO, Info] = _.evalMap { i =>
    IO {
      i.`type` match {
        case WebDev.CSS =>
          println("recevied into stream ....")
          cssIn.publish(Stream.emit(i))
        case WebDev.JS =>
          jsIn.enqueue(Stream.emit(i))
      }
    }
  }

  val stream: Stream[IO, Unit] =
    serverWithLocalAddress[IO](new InetSocketAddress(InetAddress.getByName(null), port)).flatMap {
      case Left(local) =>
        println("binding .." + local)
        Stream.eval_(localBindAddress.complete(local))
      case Right(socketHandle) =>
        socketHandle.map { socket =>
          //This is probably only used in testing or if you want to turn off the server??? .onFinalize(socket.endOfOutput)
          // through(hasChanged).filter(_.content.isEmpty) // used as a simple cache not to process already existing stylesheets ...
          socket.reads(1024).chunks.map(_.toArray).through(extractBytes).observe(splitInputStream).to(cssIn.publish) ++
            Stream.chunk(Chunk.bytes("Received Data".getBytes)).covary[IO].to(socket.writes()).drain.onFinalize(socket.endOfOutput)

        }
    }.joinUnbounded

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