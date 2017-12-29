import $ivy.`org.scodec::scodec-bits:1.1.5`
import $ivy.`org.scodec::scodec-core:1.10.3`

import java.io.{File, PrintStream}
import java.net.{InetAddress, Socket}
import scala.io.BufferedSource

import scodec.bits.BitVector
//import scodec.Attempt
import scodec._
import scodec.codecs._

// You find this datastructure in the ILib project
// Declaring it here to make the script free from
// other dependencies
case class Info(
    id: String,
    `type`: String = "CSS",
    hash: Int,
    outputPath: String,
    domElementIndex: Int,
    content: String,
)

implicit val infoCodec: Codec[Info] = {
    utf8_32 :: utf8_32 :: int32 :: utf8_32 :: int32 :: utf8_32
  }.as[Info]

val s = new Socket(InetAddress.getByName("localhost"), 6000)
val out = new PrintStream(s.getOutputStream())

val smallStyleSheetData =
  """
    |.intro {
    |  border: solid 2px red;
    |  outline: dashed;
    |}
    |
    |
    |ul:link {
    |  outline: double;
    |  display: flex;
    |}
  """.stripMargin

val styleSheet: Attempt[BitVector] =
  infoCodec.encode(Info(
    "lib",
    "CSS",
    smallStyleSheetData.hashCode(),
    "lib.css",
    -1,
    smallStyleSheetData
  ))

println("sending css ..")

out.write(styleSheet.require.toByteArray)
out.flush()

Thread.sleep(1000)
s.getInputStream.close()

out.close()
s.close()

