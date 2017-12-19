package iwebdev

import cats.effect.IO
import fs2.Pipe
import iwebdev.model.WebDev.Info

package object server {

  def log(action: String): Pipe[IO, Info, Info] =
    _.evalMap (s => IO { println(s); s})

  def showCssBlock(action: String, styleSheet: Info) = IO {
    println(s"$action > $styleSheet")
  }

}
