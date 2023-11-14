package io.findify.s3mock

import org.apache.pekko.util.ByteString
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by shutty on 8/11/16.
  */
class ChunkBufferTest extends FlatSpec with Matchers {
  "chunk buffer" should "detect header" in {
    val cb = new ChunkBuffer()
    cb.addChunk(ByteString("3;chunk-signature=1234567890123456789012345678901234567890123456789012345678901234\r\nfoo\r\n"))
    cb.readHeader shouldBe Some(Header(3, 84, "1234567890123456789012345678901234567890123456789012345678901234"))
  }
  it should "fail on non-complete header" in {
    val cb = new ChunkBuffer()
    cb.addChunk(ByteString("3;chunk-signature=123456789012345678901234567890123456789012345678901234567890"))
    cb.readHeader shouldBe None
  }
  it should "pull complete chunks" in {
    val cb = new ChunkBuffer()
    cb.addChunk(ByteString("3;chunk-signature=1234567890123456789012345678901234567890123456789012345678901234\r\nfoo\r\n"))
    val header = cb.readHeader.get
    val chunk = cb.pullChunk(header)
    chunk shouldBe Some(ByteString("foo"))
  }
  it should "ignore incomplete chunks" in {
    val cb = new ChunkBuffer()
    cb.addChunk(ByteString("3;chunk-signature=1234567890123456789012345678901234567890123456789012345678901234\r\nfo"))
    val header = cb.readHeader.get
    val chunk = cb.pullChunk(header)
    chunk shouldBe None
  }
}
