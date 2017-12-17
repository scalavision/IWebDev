package sljs

import java.io.Writer

import org.scalajs.core.tools.linker
import linker.StandardLinker
import linker.standard.OutputMode
import org.scalajs.core.tools.io.{IRFileCache, WritableMemVirtualJSFile, WritableVirtualJSFile}
import org.scalajs.core.tools.logging.ScalaConsoleLogger

//import linker.standard.StandardLinkerConfigStandardOps

import sbt._
import sbt.Keys._
import org.scalajs.sbtplugin.ScalaJSPlugin
import fs2._
import cats.effect.IO


object SocketLinkerJSPlugin extends AutoPlugin {
  override def requires = ScalaJSPlugin

  val linker = StandardLinker.apply(
    StandardLinker.Config().withOptimizer(false)
      .withSourceMap(false)
      .withPrettyPrint(true)
  )

  object autoImport {
    val pushJS = taskKey[Unit]("Test the plugin actually works")
  }

  import autoImport._
  import ScalaJSPlugin.AutoImport._

  val sljsSettings = Seq(
    pushJS := {
      println("Pushing JS")

      val irFiles = (scalaJSIR in Compile).value

      //      println("irFiles: " + irFiles.data.mkString("\n"))

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

  )

  override def projectSettings = sljsSettings
}
