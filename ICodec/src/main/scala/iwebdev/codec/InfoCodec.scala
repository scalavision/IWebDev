package iwebdev.codec

import iwebdev.model.WebDev
import scodec.codecs.{int32, utf8_32}
import iwebdev.model.WebDev.{Info, InfoType}
import scodec.{Attempt, Codec, DecodeResult}
import scodec.codecs._
import scodec.codecs.implicits._
import scodec.bits._

object InfoCodec {

  def enc(
    id: String,
    content: String,
    infoType: InfoType,
    outputPath: String
  ): Attempt[BitVector] =
    infoType match {
      case WebDev.JS =>
        infoCodec.encode(WebDev.createInfo(
          id,outputPath, content, WebDev.JS
        ))
      case WebDev.CSS =>
        infoCodec.encode(WebDev.createInfo(
          id,outputPath, content, WebDev.CSS
        ))
    }

  def dec(bitVector: BitVector): Attempt[DecodeResult[Info]] =
    infoCodec.decode(bitVector)

  val infoCodec: Codec[Info] = {
    utf8_32 :: utf8_32 :: int32 :: utf8_32 :: utf8_32
  }.as[Info]

}