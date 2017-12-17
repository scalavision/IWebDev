package fs2demo

import scala.concurrent.duration._
import Resources._
import cats.effect.IO

object Fs2Demo extends App {

  Program.cssProgram.run.unsafeRunSync()

}


