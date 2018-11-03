package iwebdev.server

import java.net.{InetAddress, InetSocketAddress}

import cats.effect.IO
import fs2.io.tcp.serverWithLocalAddress
import fs2.{Chunk, Pipe, Stream}
import iwebdev.model.WebDev
import scodec.bits.BitVector
import scodec.stream.toLazyBitVector
import fs2.concurrent.Topic
import iwebdev.model.WebDev.Info
import cats.effect.concurrent.Deferred
import Resources._

/**
  * Server receiving [[Info]] objects that could be either javascript or css chunks
  *
  * The deserialization from Array[Byte] into [[WebDev.Info]] is using a streaming decoder,
  * this is not thoroughly tested, and don't know if this is the most efficient way ...
  * It seems to be able to compose the binary stream into valid Info objects though, so happy for now ...
  *
  * @param infoInQ  Handles all incoming Info packages
  */
class CssRawIn(infoInQ: Topic[IO, Info]) {

  val port = 6000

  val localBindAddress =
    Deferred[IO, InetSocketAddress].unsafeRunSync()

  val convertFromBytesToInfo: Pipe[IO, Array[Byte], WebDev.Info] = s => InfoDecoder.streamDecoder.decode[IO] {

    toLazyBitVector {
      s.map { bb => BitVector.apply(bb) }
    }
  }

  val stream: Stream[IO, Unit] =
    serverWithLocalAddress[IO](new InetSocketAddress(InetAddress.getByName(null), port)).flatMap {
      case Left(local) =>
        println("binding webdev server for css input .." + local)
        Stream.eval_(localBindAddress.complete(local))
      case Right(socketHandle) =>
        println("got a chunk of data ..")
        Stream.resource(socketHandle).map { socket =>
          socket.reads(1024)
            .chunks.map(_.toArray).through(convertFromBytesToInfo).to(infoInQ.publish) ++
            Stream.chunk(Chunk.bytes("Received Data".getBytes))
              .covary[IO].to(socket.writes())
              .drain.onFinalize(socket.endOfOutput)
        }
    }.parJoinUnbounded
}
/*

TODO: Future improvement, implement hashtable/cache, only updating changed info content (using hashcode to achieve this)

  //  concept of filtering out unchanged info content
  // through(hasChanged).filter(_.content.isEmpty) // used as a simple cache not to process already existing stylesheets ...

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