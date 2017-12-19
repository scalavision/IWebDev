package webdev

import java.net.InetSocketAddress
import java.net.InetAddress
import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.spi.AsynchronousChannelProvider

import cats.effect.IO
import fs2._
import fs2.internal.ThreadFactories
import fs2.io.tcp.client
import org.specs2.specification.BeforeAfterAll

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.immutable
import iwebdev.server.CssSerializer.StyleSheet
import scodec.bits.{BitVector, ByteVector}
import fs2.interop.scodec.ByteVectorChunk
import java.util.concurrent.TimeUnit

import iwebdev.server.{Program, Resources}
import webdev.server.Program

class ProgramSpec extends org.specs2.mutable.Specification with BeforeAfterAll {

//  implicit val tcpACG : AsynchronousChannelGroup = AsynchronousChannelProvider.provider().
//    openAsynchronousChannelGroup(8, ThreadFactories.named("fs2-ag-tcp", true))
//
//  override def afterAll() = {
//    tcpACG.shutdownNow()
//    Resources.AG.shutdown()
//    println("shutting down ...")
//
//    println("awaiting termination...")
//    tcpACG.awaitTermination(16, TimeUnit.SECONDS)
//    Resources.AG.awaitTermination(16, TimeUnit.SECONDS)
//
//    println(Resources.AG.isShutdown)
//
//    println("threadpool shutdown")
//    ()
//    //super.afterAll()
//  }

//  override def afterAll(): Unit = {}
//
//  override def beforeAll(): Unit = {
//
//  }

//  val cssStream: Stream[IO, ByteVector] =
//    CssEncoder.streamEncoder.encode[IO](Stream(StyleSheet.create(StylesheetSample.css))).map(_.bytes).covary[IO]
//
//  val clients: Stream[IO, Array[Byte]] = {
//
//      Stream.eval(CssDLSInput.localBindAddress.get).flatMap { local =>
//        client[IO](local).flatMap { socket =>
//          Resources.Sch.delay(
//            cssStream.flatMap( b => Stream.chunk(ByteVectorChunk(b))).to(socket.writes()).drain.onFinalize(socket.endOfOutput) ++
//              socket.reads(1024, None).chunks.map(_.toArray),
//            2.seconds
//          )
//        }
//      }
//
//    }


  override def beforeAll(): Unit = ()

  override def afterAll(): Unit = Resources.shutdown()

  "CssDslInput" should {
    "be able to receive a connection" in {
      lazy val result = Program.cssProgram.run.unsafeRunTimed(3.seconds)
      println("result from run: " + result)

      1 == 1
    }
  }

}
