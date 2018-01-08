package iwebdev.server

import java.lang.Thread.UncaughtExceptionHandler
import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Executors, ThreadFactory, TimeUnit}

import cats.effect.IO
import fs2.{Scheduler, Sink}
import iwebdev.model.WebDev

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

/**
  * Handles the execution context and channel groups. Probably are better ways
  * of doing this ...
  *
  * It is inspired by fs2-http spec setup ...
  */

object Resources {

  def mkThreadFactory(name: String, daemon: Boolean, exitJvmOnFatalError: Boolean = true): ThreadFactory = {
    new ThreadFactory {
      val idx = new AtomicInteger(0)
      val defaultFactory = Executors.defaultThreadFactory()
      def newThread(r: Runnable): Thread = {
        val threadFromDefaultfactory= defaultFactory.newThread(r)
        threadFromDefaultfactory.setName(s"$name-${idx.incrementAndGet()}")
        threadFromDefaultfactory.setDaemon(daemon)
        threadFromDefaultfactory.setUncaughtExceptionHandler(new UncaughtExceptionHandler {
          def uncaughtException(t: Thread, e: Throwable): Unit = {
            ExecutionContext.defaultReporter(e)
            if (exitJvmOnFatalError) {
              e match {
                case NonFatal(_) => ()
                case fatal => System.exit(-1)
              }
            }
          }
        })
        threadFromDefaultfactory
      }
    }
  }

  implicit val EC: ExecutionContext = ExecutionContext.fromExecutor(
    Executors.newFixedThreadPool(
      8, mkThreadFactory(
        "fs2-http-spec-ec", daemon = true
      )
    )
  )

  implicit val Sch: Scheduler = Scheduler.fromScheduledExecutorService(
    Executors.newScheduledThreadPool(
      4, mkThreadFactory("fs2-http-spec-scheduler", daemon = true)
    )
  )

  implicit val AG: AsynchronousChannelGroup = AsynchronousChannelGroup.withThreadPool(
    Executors.newCachedThreadPool(
      mkThreadFactory("fs2-http-spec-AG", daemon = true)
    )
  )

  def logger(prefix: String): Sink[IO, WebDev.Info] = _.evalMap { i =>
    IO {
      println(s"$prefix > ${i.id}")
    }
  }

  def shutdown(): Unit = {
    println("shutting down!")
    AG.shutdownNow()
    println("awaiting termination ....")
    AG.awaitTermination(10, TimeUnit.SECONDS)
    println("has shutdown: " + AG.isShutdown())
    println("has terminated: " + AG.isTerminated())
  }

}
