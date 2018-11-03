package iwebdev.server

import java.net.InetSocketAddress

import cats.effect._
import fs2.io.tcp
import fs2.{Chunk, Sink, Stream}
import iwebdev.model.WebDev.Info
import Resources._
import fs2.concurrent._
import fs2.io.tcp.Socket
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
  def log(prefix: String): Sink[IO, Info] = _.evalMap { s =>
    IO {
      println(s"$prefix > " + s.id)
      infoCache += (s"/*${s.id}*/" -> s)
    }
  }

  val client: Resource[IO, Socket[IO]] = tcp.client[IO](new InetSocketAddress("127.0.0.1", 5000))

  def cssData() =
    in.subscribe(2)
      .filter(i => i.`type` == WebDev.CSS)
      .observe(log("caching")).map { info =>
      Chunk.bytes((s"/*${info.id}*/" + info.content).getBytes())
    }

  val message = Chunk.bytes("fs2.rocks".getBytes)


  def stream: Stream[IO, Unit] = Stream.resource(client).flatMap { socket =>

    Stream(
      // reading from the topic, filtering out the initial topic, and using only the CSS `type`
      // We add the info id to top of stylesheet to be able to identify it when it returns
      cssData().map { c => socket.write(c).unsafeRunAsync(println) }.drain.onFinalize(socket.endOfOutput),

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

    ).parJoin(4)

  }
}


  /* Simple example taken from the spec ...

    val clients = {
    Stream
      .range(0, 1)
      .map { idx =>
        println("happening")
        Stream.eval(localBindAddress.get).flatMap { local =>
          println("local is " + local)
          Stream.resource(client).flatMap { socket =>
            Stream
              .chunk(message)
              .to(socket.writes())
              .drain
              .onFinalize(socket.endOfOutput) ++
              socket.reads(1024, None).chunks.map(_.toArray)
          }
        }.drain
      }
      .parJoin(4)
  }.compile.drain.unsafeRunAsync(println)
*/
    /*

    Previous working code with 0.10 series of fs2 ...
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

