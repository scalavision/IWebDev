package iwebdev.server

import java.net.InetSocketAddress

import cats.effect.IO
import fs2.io.tcp
import fs2.{Chunk, INothing, Pure, Sink, Stream}
import iwebdev.model.WebDev.Info
import Resources._
import cats.effect.concurrent.Deferred
import fs2.concurrent._
import fs2.text
import iwebdev.model.WebDev

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

/*
We are using a simple infoCache for now, had a cssCache enqueue / dequeue strategy but it got
removed due to a bit misconception about buffer size in socket reads. We keep it like this for now,
but if there are problems we might add the cssCache again.

Having a subscription of only 1 item may solve the problem of rewriting the cache before getting
result back from node js.
 */
class NodeJSClient (in: Topic[IO, Info], cssCache: Queue[IO, Info], out: Queue[IO, Info]) {

  type Id = String
  var infoCache: Map[Id, Info] = Map.empty[String, Info]

  // TODO: Implement with the Map above, use the comment of Info id to extract the correct returned postprocessed stylesheet
//  var infoCache: Info = null

  val localBindAddress =
    Deferred[IO, InetSocketAddress].unsafeRunSync()

  def log(prefix: String): Sink[IO, Info] = _.evalMap { s =>

    IO {
      println(s"$prefix > " + s.id)
      infoCache += (s"/*${s.id}*/" -> s)
    }

    
  }

  val client = tcp.client[IO](new InetSocketAddress("127.0.0.1",5000))

  val message: Chunk[Byte] = Chunk.bytes("fs2.rocks".getBytes)

  val stream: Stream[IO, Unit] = Stream.eval(localBindAddress.get).flatMap { local =>
    Stream.resource(fs2.io.tcp.client[IO](local)).flatMap { socket =>
      val dataOut: Stream[IO, Chunk[Byte]] = in.subscribe(10).map { info =>
        Chunk.bytes((s"/*${info.id}*/" + info.content).getBytes())
      }
      println("Starting the node js client stuff ...")
      dataOut
        .map(socket.write(_))
        .drain.onFinalize(socket.endOfOutput) ++
        socket.reads(1024, None).through(text.utf8Decode andThen CssSerializer.splitCssChunks).evalMap { t =>
          IO {
            val id = t.lines.toList.head
            println("recieved postprocessed css: " + id)
            val cssInfo = infoCache(id)
            cssInfo.copy( content = t )
          }

        }.to(out.enqueue).drain

    }
  }.drain

  val stream2: Stream[IO, Array[Byte]] = Stream.eval(localBindAddress.get).flatMap { local =>
    Stream.resource(fs2.io.tcp.client[IO](local)).flatMap { socket =>
      val dataOut: Stream[IO, Chunk[Byte]] = in.subscribe(10).map { info =>
        Chunk.bytes((s"/*${info.id}*/" + info.content).getBytes())
      }

      dataOut
        .map(socket.write(_))
        .drain.onFinalize(socket.endOfOutput) ++
        socket.reads(1024, None).chunks.map(_.toArray)

    }
  }

//
//  val test2 : Stream[IO, Array[Byte]] = {
//    Stream
//      .range(0, 2)
//      .map { idx =>
//        Stream.eval(localBindAddress.get).flatMap { local =>
//          Stream.resource(fs2.io.tcp.client[IO](local)).flatMap { socket =>
//            val dataChunk: Stream[Array, Byte] = Stream.chunk(message)
//
//            val dataFlow: Stream[IO, Chunk[Byte]] = in.subscribe(10).map(_.toString.getBytes()).map(Chunk.bytes)
//
//            val dataFlow2: Stream[IO, Chunk[Byte]] = in.subscribe(10).map { info =>
//              Chunk.bytes((s"/*${info.id}*/" + info.content).getBytes())
//            }
//
//            dataFlow2.flatMap { c =>
//              socket.write(c)
//              Stream(c)
//            }
//
//            dataFlow2.map(socket.write(_)).drain.onFinalize(socket.endOfOutput) ++ socket.reads(1024, None).chunks.map(_.toArray)
//
////              dataFlow2.map(Stream.eval).flatMap(_.to(socket.writes()))
//              Stream.chunk(message).to(socket.writes()).drain
//              .onFinalize(socket.endOfOutput) ++
//              socket.reads(1024, None).through(text.utf8Decode andThen CssSerializer.splitCssChunks).chunks.map(_.toArray)
//
//          }
//
//        }
//      }
//      .parJoin(10)
//  }

    /*
    Stream.resource(tcp.client[IO](new InetSocketAddress("127.0.0.1",5000))).flatMap { socket =>
      val toNode: Stream[IO, Array[Byte]] = in.subscribe(10)
        .filter(i => i.`type` == WebDev.CSS).map(info => (s"/*${info.id}*/" + info.content).getBytes())
        //.map( info => Chunk.bytes((s"/*${info.id}*/" + info.content).getBytes())).map(_.toArray)

      val chunks: Stream[IO, Array[Byte]] = in.subscribe(10).map(_.toString().getBytes()) //.to(socket.writes())

      toNode.map(b => Array(b)).through(socket.writes()).last.onFinalize(socket.endOfOutput) ++ socket.reads(1024, None).chunks.map(_.toArray)

  }*/

//  val stream = Stream[IO, Array[Byte]] = {
//    Stream
//      .range(0, 1)
//      .map { idx =>
//        Stream.eval(localBindAddress.get).flatMap { local =>
//          Stream.resource(client[IO](local)).flatMap { socket =>
//            in.subscribe(10).filter(i => i.`type` == WebDev.CSS).observe(log("caching")).flatMap { s =>
//              Stream.chunk(Chunk.bytes((s"/*${s.id}*/" + s.content).getBytes()))
//            }.to(socket.writes())
//              .drain
//              .onFinalize(socket.endOfOutput) ++
//              socket.reads(1024, None) // The received css from node is separated by `>>>`, we split the chunks here ...
//              .through(text.utf8Decode andThen CssSerializer.splitCssChunks)
//              .evalMap { t =>
//
//                IO {
//
//                  val id = t.lines.toList.head
//                  println("recieved postprocessed css: " + id)
//
//                  val cssInfo = infoCache(id)
//                  cssInfo.copy(
//                    content = t
//                  )
//
//                }
//
//              }.to(out.enqueue).drain
//          }
//        }
//      }
//
//  }.parJoin(10)


  //  val stream: Stream[IO, Unit] =
//    tcp.client[IO](new InetSocketAddress("127.0.0.1",5000)).flatMap { socket =>
//
//      Stream(
//
//      ).drain
//    }

    /*
    tcp.client[IO](new InetSocketAddress("127.0.0.1", 5000)).flatMap { socket =>

      // We create a Stream of all the socket side effects, and caching of the Info object
      Stream(
        // reading from the topic, filtering out the initial topic, and using only the CSS `type`
        // We add the info id to top of stylesheet to be able to identify it when it returns
        in.subscribe(10).filter(i => i.`type` == WebDev.CSS).observe(log("caching")).flatMap { s =>
          Stream.chunk(Chunk.bytes((s"/*${s.id}*/" + s.content).getBytes()))
        }.to(socket.writes()).drain,

        socket.reads(1024, None)
          // The received css from node is separated by `>>>`, we split the chunks here ...
          .through(text.utf8Decode andThen CssSerializer.splitCssChunks)
          .evalMap { t =>

            IO {

            val id = t.lines.toList.head
            println("recieved postprocessed css: " + id)

              val cssInfo = infoCache(id)
              cssInfo.copy(
                content = t
              )

            }

          }.to(out.enqueue).drain

      ).parJoin(2)

    }*/


  //  def log(prefix: String): Sink[IO, Info] = _.evalMap { s =>
//    IO {
//      infoCache = (s.id -> s) + infoCache
//      println(s"$prefix > ${s.id}")
//    }
//  }

//  val stream: Stream[IO, Unit] =
//    tcp.client[IO](new InetSocketAddress("127.0.0.1", 5000)).flatMap { socket =>
//
//      // We create a Stream of all the socket side effects, and caching of the Info object
//      Stream(
//        // reading from the topic, filtering out the initial topic, and using only the CSS `type`
//        in.subscribe(1).filter(i => i.`type` == WebDev.CSS).flatMap { s =>
//          Stream.chunk(Chunk.bytes(s.content.getBytes()))
//        }.to(socket.writes()),
//        socket.reads(16, None)
//          // The received css from node is separated by `>>>`, we split the chunks here ...
//          .through(text.utf8Decode andThen CssSerializer.splitCssChunks)
//            .zip(in.subscribe(1))
//          .flatMap { t =>
//
//            val css = t._1
//            val info = t._2
//
//            Stream.eval { IO {
//              info.copy(
//                  content = css
//              )}}
//
//          }.to(out.enqueue).drain
//
//      ).join(3)
//
//    }


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
