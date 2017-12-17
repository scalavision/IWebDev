package sljs

import cats.effect.IO
import fs2demo.Program
import fs2._
import fs2demo.CssSerializer.StyleSheet
import org.scalajs.core.tools.linker
import linker.StandardLinker
import org.scalajs.core.tools.io.WritableMemVirtualJSFile
import org.scalajs.core.tools.logging.ScalaConsoleLogger
import sbt._
import org.scalajs.sbtplugin.ScalaJSPlugin

object SocketLinkerJSPlugin extends AutoPlugin {
  override def requires = ScalaJSPlugin

  val linker = StandardLinker.apply(
    StandardLinker.Config().withOptimizer(false)
      .withSourceMap(false)
      .withPrettyPrint(true)
  )

  object autoImport {
    val pushJS = taskKey[Unit]("Test the plugin actually works")
    val startDevServer = taskKey[Unit]("Start the dev server")
  }

  import autoImport._
  import ScalaJSPlugin.AutoImport._

  val jsQueue = Stream.eval(async.boundedQueue(100)[IO, String])

  val sljsSettings = Seq(
    pushJS := {
      println("Pushing JS")

      val irFiles = (scalaJSIR in Compile).value

      val modules = scalaJSModuleInitializers.value
      println("modules: " + modules.mkString("\n"))

      println("trying to run the linker:")

      val output = WritableMemVirtualJSFile.apply("test")

      linker.link(
        irFiles.data,
        modules,
        output,
        new ScalaConsoleLogger()
      )

      println("the linker was run!")
      println(output.content)

      val in = for {
        q <- jsQueue
        p = q.enqueue(Stream.emit(output.content))
      } yield p

      in.run.unsafeRunSync()

    },
    startDevServer := {

    val out  =  for {
        q <- jsQueue
        p = Program.cssProgram(q)
      } yield p

      out.run.unsafeRunSync()
    }

  )

  override def projectSettings = sljsSettings

}



//      val filerOwnCode: Pipe[IO, String, String] = _.evalMap { l =>
//      IO {
//        val text = ""
////        for (char <- Seq("d", "c", "h", "i", "n", "m")) {
////          if(l.startsWith(s"""$$${char}_L""")) {
////            text + l
////          } else {
////            text
////          }
////        }
//
//        if(l.startsWith("""$c_L""")) {
//          text + l
//        } else
//          text
//
//      }}
//
//      val log: Sink[IO, String] = _.evalMap { s =>
//        IO{
//          println(s)
//        }
//      }
//
//      Stream.emit(output.content)
//        .through(text.lines).covary[IO]
//        .through(filerOwnCode)
//        .to(log).run.unsafeRunSync()
//
//    }

