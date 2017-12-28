package iwebdev.server

import cats.effect.IO
import fs2.Pipe
import iwebdev.model.WebDev
import scodec.bits._
import scodec.codecs._
import scodec.stream.{StreamDecoder, StreamEncoder, decode, encode}


/**
  * Creating a streaming codec for [[WebDev.Info]] objects
  * Also splitting the returned postprocessed css from node server.
  *
  * It is splitted on `<<<`
  *
  */
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

}

object InfoDecoder {
  import iwebdev.codec.InfoCodec._
  val streamDecoder: StreamDecoder[WebDev.Info] = decode.many(infoCodec)
}

object InfoEncoder {
  import iwebdev.codec.InfoCodec._
  val streamEncoder: StreamEncoder[WebDev.Info] = encode.many(infoCodec)
}
