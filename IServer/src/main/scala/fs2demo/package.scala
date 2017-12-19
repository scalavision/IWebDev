import cats.effect.IO
import fs2.Pipe
import fs2demo.CssSerializer.StyleSheet

package object fs2demo {

  def log(action: String): Pipe[IO, StyleSheet, StyleSheet] =
    _.evalMap (s => IO { println(s); s})

  def showCssBlock(action: String, styleSheet: StyleSheet) = IO {
    println(s"$action > $styleSheet")
  }

}
