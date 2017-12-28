package sljs

import java.io.{File, PrintStream}
import java.net.{InetAddress, Socket}
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
import iwebdev.server.Resources._

/**
  * This sbt plugin is `WIP`. It takes the output from fastOptJS task wraps it
  * into a [[iwebdev.model.WebDev.Info]] object and sends it to a
  * running instance of Instant WebDev Server [[iwebdev.server.WebDevServer]]
  * on port 6000.
  *
  * To use the plugin in your ScalaJS web client issue:
  *
  * ~pushToClient
  */
object IWebDevPlugin extends AutoPlugin {

  override def requires = ScalaJSPlugin

  val log: ConsoleLogger = ConsoleLogger.apply()

  val linker = StandardLinker.apply(
    StandardLinker.Config().withOptimizer(false)
      .withSourceMap(false)
      .withPrettyPrint(true)
  )

  object autoImport {
    val domNodeId = settingKey[String]("The javascript dom node id attribute, project name is used as default")
    val outputJSPath = settingKey[File]("Output path of the Javascript file, project root is used as default")
    val outputJSFilename = settingKey[String]("Output path of the Javascript file, project name is used as default")
    val saveJS = taskKey[Unit]("Save compiled javascript client to path, this is `WIP`")
    val pushToClient = taskKey[Unit]("Linking and then push compiled javascript to client using an Info object")
    val startDevServer = taskKey[Unit]("Start the instant webdev server, this is `WIP`")
  }

  import autoImport._
  import ScalaJSPlugin.AutoImport._

  implicit val serializer  = InfoCodec.infoCodec

  val sljsSettings = Seq(
    outputJSPath := new java.io.File("."),
    outputJSFilename := {
      Keys.name.value
    },
    domNodeId := {
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


      val s = new Socket(InetAddress.getByName("localhost"), 6000)
      val out = new PrintStream(s.getOutputStream())

      log.info("linking ..")

      linker.link(
        irFiles.data,
        modules,
        output,
        new ScalaConsoleLogger()
      )

      log.info("finished ..")

      val packet = WebDev.createInfo(
        domNodeId.value,
        (outputJSPath.value / outputJSFilename.value).getAbsolutePath,
        output.content,
        WebDev.JS
      )

      out.write(
        serializer.encode(packet).require.toByteArray
      )

      log.info("sending javascript to WebDev server ..")
      out.flush()

      Thread.sleep(1000)
      s.getInputStream.close()

      out.close()
      s.close()

      log.info("sent, socket and stream closed ..")


    },
    startDevServer := {
//      log.info("The Instant WebDev Server support is still WIP ..")
      log.info("staring Instant WebDev Server ..")
      Program.processInfoStream.run.unsafeRunAsync(println)
      log.info("Instant WebDev Server has started ..")
    }

  )

  override def projectSettings = sljsSettings

}

