package sljs

import cats.effect.IO
import fs2demo.Program
import fs2._
import fs2demo.CssSerializer.StyleSheet
import iwebdev.codec.InfoCodec
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
    val outputJSPath = settingKey[String]("Output path of the Javascript file")
    val saveJS = taskKey[Unit]("Save compiled javascript client to path")
    val pushToClient = taskKey[Unit]("Push compiled javascript to client")
    val startDevServer = taskKey[Unit]("Start the instant webdev server")
  }

  import autoImport._
  import ScalaJSPlugin.AutoImport._

  implicit val serializer  = InfoCodec.infoCodec


  val sljsSettings = Seq(
    outputJSPath := "test.js",
    saveJS := {

      val irFiles = (scalaJSIR in Compile).value
      val modules = scalaJSModuleInitializers.value
      val output = WritableMemVirtualJSFile.apply("test")

      def link() = linker.link(
        irFiles.data,
        modules,
        output,
        new ScalaConsoleLogger()
      )


      println("Linking JS")
      link()
      sbt.IO.write(
        new java.io.File(outputJSPath.value),
        output.content,
        java.nio.charset.StandardCharsets.UTF_8
      )
    },
    pushToClient := {

      val irFiles = (scalaJSIR in Compile).value
      val modules = scalaJSModuleInitializers.value
      val output = WritableMemVirtualJSFile.apply("test")

      def link() = linker.link(
        irFiles.data,
        modules,
        output,
        new ScalaConsoleLogger()
      )


      println("Linking JS")
      link()
      println("the linker was run!")
      println(output.content)

    },
    startDevServer := {
      Program.cssProgram.run.unsafeRunSync()
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

