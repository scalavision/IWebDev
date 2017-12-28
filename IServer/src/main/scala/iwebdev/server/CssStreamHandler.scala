package iwebdev.server

import fs2.{Chunk, Pipe, Pull, Stream}

/** Not in Use !
  *
  * splits all css blocks on  closing '}' bracket. For each '}' it counts backwards to see if they
  * align, if so it creates a Chunk containing the css block.
  *
  */

object CssStreamHandler {

  /** Transforms a stream of `String` such that each emitted `String` is a line from the input. */
  def cssBlocks[F[_]]: Pipe[F, String, String] = {

    def linesFromString(string: String, collectedCssInBlock: String): (Vector[String], String) = {

      var i = 0
      var start = 0
      var out = Vector.empty[String]

      def count(openOrCloseBracket: Char) =
        collectedCssInBlock.count(_ == openOrCloseBracket) +
          string.substring(start, i).count(_ == openOrCloseBracket) +
          out.headOption.fold(0)(s => s.count(_ == openOrCloseBracket))

      while (i < string.size) {

        string(i) match {

          case '}' =>

            if(count('{') == count('}') + 1) {
              out = out :+ ("\n\r" + string.substring(start, i + 1) + "\n\r").trim() //:+ string.substring(i - 1, i)
              start = i + 1
            }
            else {
              ()
            }

          case c =>
            ()
        }

        i += 1
      }
      val carry = string.substring(start, string.size)
      (out, carry.trim())
    }

    def extractLines(buffer: Vector[String], chunk: Chunk[String], pendingLineFeed: Boolean): (Chunk[String], Vector[String], Boolean) = {
      @annotation.tailrec
      def loop(remainingInput: Vector[String], buffer: Vector[String], output: Vector[String], pendingLineFeed: Boolean): (Chunk[String], Vector[String], Boolean) = {
        if (remainingInput.isEmpty) {
          (Chunk.indexedSeq(output), buffer, pendingLineFeed)
        } else {
          val next = remainingInput.head
          if (pendingLineFeed) {
            if (next.headOption == Some('}')) {
              val out = (buffer.init :+ buffer.last.init).mkString
              loop(next.tail +: remainingInput.tail, Vector.empty, output :+ out, pendingLineFeed = false)
            } else {
              loop(remainingInput, buffer, output, pendingLineFeed = false)
            }
          } else {
            val (out, carry) = linesFromString(next, if(buffer.isEmpty) "" else buffer.mkString)
            val pendingLF = if (carry.nonEmpty) carry.last == '}' else pendingLineFeed
            loop(remainingInput.tail,
              if (out.isEmpty) buffer :+ carry else Vector(carry),
              if (out.isEmpty) output else output ++ ((buffer :+ out.head).mkString +: out.tail), pendingLF
            )
          }
        }
      }
      loop(chunk.toVector, buffer, Vector.empty, pendingLineFeed)
    }

    def go(buffer: Vector[String], pendingLineFeed: Boolean, s: Stream[F, String]): Pull[F, String, Option[Unit]] = {
      s.pull.unconsChunk.flatMap {
        case Some((chunk, s)) =>
//          println("got some ...")
//          pprint.pprintln(chunk.toString())
          val (toOutput, newBuffer, newPendingLineFeed) = extractLines(buffer, chunk, pendingLineFeed)
          Pull.output(toOutput.toSegment) >> go(newBuffer, newPendingLineFeed, s)
        case None if buffer.nonEmpty => Pull.output1(buffer.mkString) >> Pull.pure(None)
        case None => Pull.pure(None)
      }
    }

    s => go(Vector.empty, false, s).stream
  }


}
