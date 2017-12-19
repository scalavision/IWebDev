package fs2demo

import cats.effect.IO
import fs2.Pipe
import scodec.{Attempt, Codec, DecodeResult}
import scodec.codecs._
import scodec.bits._
import scodec.codecs.implicits._
import scodec.stream.{StreamDecoder, StreamEncoder, decode, encode}


object CssSerializer {

  sealed trait Css
  case class PreCss(s: StringBuilder) extends Css
  case class PostCss(s: String, rest: String) extends Css

  val filterSplit = (s: String) => s.dropWhile(_ != '<').drop(3)

  val splitCssChunks: Pipe[IO, String, String] = {
    _.scan(
    PreCss(new StringBuilder("")
    ): Css) {
    case (PreCss(buf), next) =>

      if(next.contains("<<<"))
        PostCss(buf.append(next.takeWhile(_ != '<')).result(), filterSplit(next))
      else
        PreCss(buf.append(next))
    case (PostCss(_, rest), next) =>

      if(next.contains("<<<"))
        PreCss(new StringBuilder(filterSplit(next)))
      else
        PreCss(new StringBuilder(rest + next))
  } collect {
      case PostCss(s, _ )=>
        IO {
          println("cssBlock: s")
        }; s
    }

  }

  case class StyleSheet(
    id: String,
    contentHash: Int,
    content: String
  )

  object StyleSheet {

    def create(css: String) = {
      StyleSheet(
        "DemoCss",
        css.hashCode(),
        css
      )
    }

    def enc(css: String): Attempt[BitVector] =
      stylesheetCodec.encode(create(css))

    def dec(bitVector: BitVector): Attempt[DecodeResult[StyleSheet]] =
      stylesheetCodec.decode(bitVector)

  }

  val stylesheetCodec: Codec[StyleSheet] = {
    utf8_32 :: int32 :: utf8_32
  }.as[StyleSheet]

}

object CssDecoder {
  import CssSerializer._
  val streamDecoder: StreamDecoder[StyleSheet] = decode.many(stylesheetCodec)
}

object CssEncoder {
  import CssSerializer._
  val streamEncoder: StreamEncoder[StyleSheet] = encode.many(stylesheetCodec)
}
