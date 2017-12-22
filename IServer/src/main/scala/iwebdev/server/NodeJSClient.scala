package iwebdev.server

import java.net.InetSocketAddress

import cats.effect.IO
import fs2.async.mutable.Queue
import fs2.io.tcp
import fs2.{Chunk, Sink, Stream, async, text}
import iwebdev.model.WebDev
import iwebdev.model.WebDev.Info
import Resources._


class NodeJSClient (in: Queue[IO, Info], out: Queue[IO, Info]) {

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

  val stream: Stream[IO, Unit] = tcp.client[IO](new InetSocketAddress("127.0.0.1", 5000)).flatMap { socket =>

      in.dequeue.map(_.content).filter(_.nonEmpty).flatMap { s => Stream.eval(socket.write(Chunk.bytes(s.getBytes)))}.drain mergeHaltBoth
        socket.reads(1024, None).through(text.utf8Decode andThen CssSerializer.splitCssChunks).map( css =>
        WebDev.createCss(
          "myId",
          "outputpath",
          css
        ).toInfo
      ).to(out.enqueue).drain

    }


  /*
  def stream(p: Pipe[IO, Info, Info]) = tcp.client[IO](new InetSocketAddress("127.0.0.1", 5000)).flatMap { socket =>




    ???
  }*/

  /*
  val stream: Stream[IO, Unit] = tcp.client[IO](new InetSocketAddress("127.0.0.1", 5000)).flatMap { socket =>

    println("opening socket ...")
    in.dequeue.map { i =>
      val t = async.topic[IO, Info](i)
      t.flatMap { inner =>

        println("inner ...")
          IO {
            inner.subscribe(1).map(_.content).filter(_.nonEmpty).flatMap { s => Stream.eval(socket.write(Chunk.bytes(s.getBytes)))} merge
            inner.subscribe(1).zip(socket.reads(1024, None).through(text.utf8Decode andThen CssSerializer.splitCssChunks)).flatMap { t =>
              Stream.eval(
                IO{
                  val postProcessedSheet = t._2
                  val oldSheet = t._1
                  WebDev.createInfo(
                    oldSheet.id,
                    oldSheet.outputPath,
                    postProcessedSheet,
                    WebDev.CSS
                  )
                }
              )
            }.to(out.enqueue).drain
          }
      }.unsafeRunSync()

    }.drain

  }*/


    /*
    in.dequeue.evalMap { i =>
      IO {

        async.topic[IO, Info](i).flatMap { s =>

          s.subscribe(1).map(_.content).filter(_.nonEmpty).flatMap { s => Stream.eval(
            socket.write(
              Chunk.bytes(
                s.getBytes
              )
            )
          )
          }
          s.subscribe(1).zip(
            socket.reads(1024, None).through(
              text.utf8Decode andThen CssSerializer.splitCssChunks
            )
          ).flatMap { t =>

            val postProcessedSheet = t._2
            val oldSheet = t._1

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

        }
      }
    }


    }



//    val repeatPoll: Stream[IO, Info] = in.dequeue.repeat
//
//    for {
//      i <- repeatPoll
//
//    }




//    async.signalOf[IO, Info](WebDev.createInit).flatMap { sig =>





//
//      val out: Stream[IO, Unit] = in.dequeue.map {i => sig.set(i); i.content }.filter(_.nonEmpty).flatMap { s => Stream.eval(socket.write(Chunk.bytes(s.getBytes))) } //merge
//
//      val in: Stream[IO, Unit] = sig.flatMap
//
//
//         //socket.reads(1024).through(text.utf8Decode andThen CssSerializer.splitCssChunks)
//
//
//
//    }


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


  val p: Pipe[IO, Socket[IO], Socket[IO]] = _.evalMap { socket =>

    IO {
      println("subscribing ...")

      in.subscribe(10).filter(_.content.nonEmpty).flatMap{ s =>
        Stream.segment(Segment(s.content))
      }.flatMap { b =>
        Stream.chunk(ByteVectorChunk(ByteVector.apply(b.getBytes)))
      }.to(socket.writes()).drain.onFinalize(socket.endOfOutput) merge in.subscribe(10).filter(_.content.nonEmpty).zip(socket.reads(1024, None).through(text.utf8Decode andThen CssSerializer.splitCssChunks)).flatMap { t =>

        println("receveing processed!")
        val postProcessedSheet = t._2
        val oldSheet = t._1

        Stream.eval(
          IO {
            WebDev.createInfo(
              oldSheet.id,
              oldSheet.outputPath,
              postProcessedSheet,
              WebDev.CSS
            )
         })
      }.to(out.enqueue).drain

      socket
    }



  }

  val stream6: Stream[IO, Unit] = tcp.client[IO](new InetSocketAddress("127.0.0.1", 5000)).flatMap { socket =>

    var cache: Info = null

    (in.subscribe(10).filter(_.content.nonEmpty).flatMap { s =>
      cache = s
      Stream.segment(Segment(s.content))
    }.flatMap { b =>
      Stream.chunk(ByteVectorChunk(ByteVector.apply(b.getBytes)))
    }.to(socket.writes()) ++ socket.reads(1024, None).through(text.utf8Decode andThen CssSerializer.splitCssChunks).flatMap { s =>
      Stream.eval( IO {
        WebDev.createInfo(
          cache.id,
          cache.outputPath,
          s,
          cache.`type`
        )
      } )
    }.observe(logInfo).to(out.enqueue))

//    in.subscribe(10).filter(_.content.nonEmpty).zip(socket.reads(1024, None).through(text.utf8Decode andThen CssSerializer.splitCssChunks)).flatMap { t =>
//
//      println("receveing processed!")
//      val postProcessedSheet = t._2
//      val oldSheet = t._1
//
//      Stream.eval(
//        IO {
//          WebDev.createInfo(
//            oldSheet.id,
//            oldSheet.outputPath,
//            postProcessedSheet,
//            WebDev.CSS
//          )
//        })
//    }.to(out.enqueue).drain


  }

  val stream9: Stream[IO, Unit] = tcp.client[IO](new InetSocketAddress("127.0.0.1", 5000)).flatMap { socket =>

    var cache: Info = null

    in.subscribe(10).filter(_.content.nonEmpty).flatMap { s =>
      cache = s
      Stream.segment(Segment(s.content))
    }.flatMap { b =>
      Stream.chunk(ByteVectorChunk(ByteVector.apply(b.getBytes)))
    }.to(socket.writes()) merge socket.reads(1024, None).through(text.utf8Decode andThen CssSerializer.splitCssChunks).flatMap { s =>
      Stream.eval( IO {
        WebDev.createInfo(
          cache.id,
          cache.outputPath,
          s,
          cache.`type`
        )
      } )
    }.observe(logInfo).to(out.enqueue).drain

    //    in.subscribe(10).filter(_.content.nonEmpty).zip(socket.reads(1024, None).through(text.utf8Decode andThen CssSerializer.splitCssChunks)).flatMap { t =>
    //
    //      println("receveing processed!")
    //      val postProcessedSheet = t._2
    //      val oldSheet = t._1
    //
    //      Stream.eval(
    //        IO {
    //          WebDev.createInfo(
    //            oldSheet.id,
    //            oldSheet.outputPath,
    //            postProcessedSheet,
    //            WebDev.CSS
    //          )
    //        })
    //    }.to(out.enqueue).drain


  }

  val stream5: Stream[IO, Unit] =
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
          }.to(out.enqueue)
          program <- Stream(nodeJS, cssProcessed, both).covary[IO]
        } yield program

    }

  val stream4: Stream[IO, Unit] =
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
    */
}
