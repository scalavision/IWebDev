package sljs

import java.io.{File, PrintStream}
import java.net.{InetAddress, Socket}

import iwebdev.codec.InfoCodec
import iwebdev.model.WebDev
import iwebdev.server.Program
import org.scalajs.core.tools.linker
import linker.{ModuleInitializer, StandardLinker}
import org.scalajs.core.tools.io.WritableMemVirtualJSFile
import org.scalajs.core.tools.logging.ScalaConsoleLogger
import sbt._
import sbt.Keys
import sbt.Keys._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.apache.logging.log4j.message._
import org.apache.logging.log4j.core.{LogEvent => Log4JLogEvent, _}
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.layout.PatternLayout
import iwebdev.server.Resources._

/**
  * This sbt plugin is `WIP`. It takes the output from fastOptJS task wraps it
  * into a [[iwebdev.model.WebDev]] Info object and sends it to a
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

  val scalaJSLogger = new ScalaConsoleLogger()


  def sendToWebDevServer(info: WebDev.Info): Unit = {

  val s = new Socket(InetAddress.getByName("localhost"), 6000)
  val out = new PrintStream(s.getOutputStream())

    out.write(
      serializer.encode(info).require.toByteArray
    )

    log.info("sending javascript to WebDev server ..")
    out.flush()

    Thread.sleep(1000)
    s.getInputStream.close()

    out.close()
    s.close()

  }

  val sljsSettings = Seq(
    outputJSPath := (baseDirectory in run).value,
    outputJSFilename := {
      Keys.name.value + ".js"
    },
    domNodeId := {
      Keys.name.value
    },
    (extraLoggers in ThisBuild) := {

      def sendLogInfo(level: String, content: String) = {
        sendToWebDevServer(WebDev.createInfo(
          id = "sbtInfo",
          outputPath = "",
          content = level + " : " + content,
          infoType = WebDev.SBT_INFO
        ))
      }
      val clientLogger = new AbstractAppender(
        "FakeAppender",
        null,
        PatternLayout.createDefaultLayout()) {
        override def append(event: Log4JLogEvent): Unit = {

          val level = sbt.internal.util.ConsoleAppender.toLevel(event.getLevel)
          val message = event.getMessage

          message match {
            case o: ObjectMessage => {
              o.getParameter match {
                case e: sbt.internal.util.StringEvent => sendLogInfo(level.toString, e.message)
                case e: sbt.internal.util.ObjectEvent[_] => sendLogInfo(level.toString, e.message.toString)
                case _ => sendLogInfo(level.toString, message.getFormattedMessage)
              }
            }
            case _ => sendLogInfo(level.toString, message.getFormattedMessage)
          }
        }
      }
      clientLogger.start()
      val currentFunction = extraLoggers.value
      (key: ScopedKey[_]) => clientLogger +: currentFunction(key)
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
      //val modules = scalaJSModuleInitializers.value
      val output = WritableMemVirtualJSFile.apply("clientInMemTmpFile")

      log.info("linking ..")

      linker.link(
        irFiles.data,
        Seq(
          ModuleInitializer.mainMethodWithArgs(
            "mp.client.MindPointer", "main"
          )),
        output,
        scalaJSLogger
      )

      log.info("finished with hash: " + output.content.hashCode())

      sendToWebDevServer(WebDev.createInfo(
        domNodeId.value,
        (outputJSPath.value / outputJSFilename.value).getAbsolutePath,
        output.content,
        WebDev.JS
      ))
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

