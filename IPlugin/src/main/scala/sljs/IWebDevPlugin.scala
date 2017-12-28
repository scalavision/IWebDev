package sljs

import java.io.{File, PrintStream}
import java.net.{InetAddress, Socket}

import cats.effect.IO
import fs2._
import iwebdev.codec.InfoCodec
import iwebdev.model.WebDev
import iwebdev.server.Program
import org.scalajs.core.tools.linker
import linker.StandardLinker
import org.scalajs.core.tools.io.WritableMemVirtualJSFile
import org.scalajs.core.tools.logging.ScalaConsoleLogger
import sbt._
import sbt.Keys
import org.scalajs.sbtplugin.ScalaJSPlugin

object IWebDevPlugin extends AutoPlugin {
  override def requires = ScalaJSPlugin

  val linker = StandardLinker.apply(
    StandardLinker.Config().withOptimizer(false)
      .withSourceMap(false)
      .withPrettyPrint(true)
  )

  object autoImport {
    val outputJSPath = settingKey[File]("Output path of the Javascript file")
    val outputJSFilename = settingKey[String]("Output path of the Javascript file")
    val saveJS = taskKey[Unit]("Save compiled javascript client to path")
    val pushToClient = taskKey[Unit]("Push compiled javascript to client")
    val startDevServer = taskKey[Unit]("Start the instant webdev server")
  }

  import autoImport._
  import ScalaJSPlugin.AutoImport._

  implicit val serializer  = InfoCodec.infoCodec

  val sljsSettings = Seq(
    outputJSPath := new java.io.File("."),
    outputJSFilename := {
      Keys.name.value
    },
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
        outputJSPath.value /  outputJSFilename.value,
        output.content,
        java.nio.charset.StandardCharsets.UTF_8
      )
    },
    pushToClient := {

      val irFiles = (scalaJSIR in Compile).value
      val modules = scalaJSModuleInitializers.value
      val output = WritableMemVirtualJSFile.apply("clientInMemTmpFile")

      def link() = linker.link(
        irFiles.data,
        modules,
        output,
        new ScalaConsoleLogger()
      )

      val s = new Socket(InetAddress.getByName("localhost"), 6000)
      //lazy val in = new BufferedSource(s.getInputStream()).getLines()
      val out = new PrintStream(s.getOutputStream())

//      val filePath = outputJSPath.value / outputJSFilename.value

//      println("filePath is: " + filePath.getAbsolutePath)

      link()

      val packet = WebDev.createInfo(
        sbt.Keys.name.value,
        "not implemented",//,
        output.content,
        WebDev.JS
      )

      out.write(
        serializer.encode(packet).require.toByteArray
      )

      out.flush()

      Thread.sleep(1000)
      s.getInputStream.close()

      out.close()
      s.close()

      println("everything has beens shutdown and resources are destroyed")

      println("Linking JS")
      println("the linker was run!")

    },
    startDevServer := {
      Program.processInfoStream.run.unsafeRunSync()
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

