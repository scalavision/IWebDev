package iwebdev.client.ws

import org.scalajs.dom.raw.Blob
import scala.scalajs.js
import scodec.bits._
import js.JSConverters._


//TODO: investigate how to make WebSocket work with FS2 HTTP lib, probably need to use a Frame.Binary
object PingFrame {
// Taken from Specs in ScalaJS, don't know how to do this yet ...
//  object Int8ArrayFactory {
//    def bytesPerElement: Int = Int8Array.BYTES_PER_ELEMENT
//    def lenCtor(len: Int): Int8Array = new Int8Array(len)
//    def tarrCtor(tarr: TypedArray[_, _]): Int8Array = new Int8Array(tarr)
//    def itCtor(arr: js.Iterable[_]): Int8Array = new Int8Array(arr)
//    def bufCtor1(buf: ArrayBuffer): Int8Array = new Int8Array(buf)
//    def bufCtor2(buf: ArrayBuffer, start: Int): Int8Array = new Int8Array(buf, start)
//    def bufCtor3(buf: ArrayBuffer, start: Int, end: Int): Int8Array = new Int8Array(buf, start, end)
//    def hasType(obj: Any): Boolean = obj.isInstanceOf[Int8Array]
//    def intToV(n: Int): Byte = n.toByte
//  }

  val maskPattern: ByteVector = hex"DEADBEEF"

  def ping = {

    // first 4 bits means this is the final frame, rsv1 - 3 is all false, the
    // ping /pong identifier, masked is true, length of data is set to 0

    val pingHeader: BitVector = bin"0000 1001 1 0000000"
    val pM = maskPattern.toArray
    val pH = pingHeader.toByteArray
    val p = pM ++ pH
    new Blob(p.map(_.asInstanceOf[js.Any]).toJSArray).close()
  }

  val pong = {
    val pingHeader: BitVector = bin"0000 1010 1 0000000"
    val pM = maskPattern.toArray
    val pH = pingHeader.toByteArray
    val p = pM ++ pH
    new Blob(p.map(_.asInstanceOf[js.Any]).toJSArray)

  }

}

/**
  * Frames a piece of data according to the HyBi WebSocket protocol.
  *
  * @param {Buffer} data The data to frame
  * @param {Object} options Options object
  * @param {Number} options.opcode The opcode
  * @param {Boolean} options.readOnly Specifies whether `data` can be modified
  * @param {Boolean} options.fin Specifies whether or not to set the FIN bit
  * @param {Boolean} options.mask Specifies whether or not to mask `data`
  * @param {Boolean} options.rsv1 Specifies whether or not to set the RSV1 bit
  * @return {Buffer[]} The framed data as a list of `Buffer` instances
  * @public

static frame (data, options) {
  const merge = data.length < 1024 || (options.mask && options.readOnly);
  var offset = options.mask ? 6 : 2;
  var payloadLength = data.length;

  if (data.length >= 65536) {
  offset += 8;
  payloadLength = 127;
} else if (data.length > 125) {
  offset += 2;
  payloadLength = 126;
}

  const target = Buffer.allocUnsafe(merge ? data.length + offset : offset);

  target[0] = options.fin ? options.opcode | 0x80 : options.opcode;
  if (options.rsv1) target[0] |= 0x40;

  if (payloadLength === 126) {
  target.writeUInt16BE(data.length, 2, true);
} else if (payloadLength === 127) {
  target.writeUInt32BE(0, 2, true);
  target.writeUInt32BE(data.length, 6, true);
}

  if (!options.mask) {
  target[1] = payloadLength;
  if (merge) {
  data.copy(target, offset);
  return [target];
}

  return [target, data];
}

  const mask = crypto.randomBytes(4);

  target[1] = payloadLength | 0x80;
  target[offset - 4] = mask[0];
  target[offset - 3] = mask[1];
  target[offset - 2] = mask[2];
  target[offset - 1] = mask[3];

  if (merge) {
  bufferUtil.mask(data, mask, target, offset, data.length);
  return [target];
}

  bufferUtil.mask(data, mask, data, 0, data.length);
  return [target, data];
}
  */