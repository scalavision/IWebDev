package iwebdev.server

import java.net.{InetAddress, InetSocketAddress}

import cats.effect.IO
import fs2.{Chunk, Pipe, Stream}
import fs2.io.tcp.serverWithLocalAddress
import iwebdev.model.WebDev
import iwebdev.model.WebDev.Info
import scodec.bits.BitVector
import scodec.stream.toLazyBitVector
import Resources._
import cats.effect.concurrent.Deferred
import fs2.concurrent.Topic

class JSRawIn(jsInfoInQ: Topic[IO, Info]) {

  val port = 6001
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
        println("binding webdev server .." + local)
        Stream.eval_(localBindAddress.complete(local))
      case Right(socketHandle) =>
        Stream.resource(socketHandle).map { socket =>

          socket.reads(4096).chunks.map(_.toArray).through(convertFromBytesToInfo).to(jsInfoInQ.publish) ++
            Stream.chunk(Chunk.bytes("Received Data".getBytes)).covary[IO].to(socket.writes()).drain.onFinalize(socket.endOfOutput)
        }

    }.parJoinUnbounded


}
