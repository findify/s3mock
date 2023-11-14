package io.findify.s3mock

import org.apache.pekko.stream._
import org.apache.pekko.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import org.apache.pekko.util.ByteString
import com.typesafe.scalalogging.LazyLogging

/**
  * Created by shutty on 8/11/16.
  */
case class Header(chunkSize:Int, headerSize:Int, sig:String)

class ChunkBuffer extends LazyLogging {
  val hexChars = "0123456789abcdef".getBytes.toSet
  var size = -1
  var buffer = ByteString("")
  def addChunk(data:ByteString) = buffer = buffer ++ data
  def readHeader:Option[Header] = {
    val headerBuffer = buffer.take(90)
    val size = headerBuffer.takeWhile(hexChars.contains)
    val sig = headerBuffer.drop(size.length).take(83)
    if ((size.length <= 8) && (sig.length == 83) && sig.startsWith(";chunk-signature=") && sig.endsWith("\r\n")) {
      val header = Header(Integer.parseInt(size.utf8String, 16), size.length + 83, sig.drop(17).dropRight(2).utf8String)
      logger.debug(s"read header: $header")
      Some(header)
    } else {
      logger.debug("cannot read header")
      None
    }
  }
  def pullChunk(header:Header):Option[ByteString] = {
    if (buffer.length >= header.headerSize + header.chunkSize + 2) {
      buffer = buffer.drop(header.headerSize)
      val chunk = buffer.take(header.chunkSize)
      buffer = buffer.drop(header.chunkSize + 2)
      logger.debug(s"pulled chunk, size=${header.chunkSize}")
      Some(chunk)
    } else {
      logger.debug(s"not enough data to pull chunk: chunkSize = ${header.chunkSize}, bufferSize = ${buffer.length}")
      None
    }
  }
}

class S3ChunkedProtocolStage extends GraphStage[FlowShape[ByteString,ByteString]] {
  val out = Outlet[ByteString]("s3.out")
  val in = Inlet[ByteString]("s3.in")
  override val shape = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {
    val buffer = new ChunkBuffer()

    setHandler(in, new InHandler {
      override def onPush() = {
        buffer.addChunk(grab(in))
        buffer.readHeader match {
          case Some(header) => buffer.pullChunk(header) match {
            case Some(chunk) => push(out, chunk)
            case None => pull(in)
          }
          case None => pull(in)
        }
      }

      override def onUpstreamFinish() = {
        buffer.readHeader match {
          case Some(header) => buffer.pullChunk(header) match {
            case Some(chunk) =>
              push(out, chunk)
              complete(out)
            case None =>
              complete(out)
          }
          case None =>
            complete(out)
        }
      }
    })
    setHandler(out, new OutHandler {
      override def onPull() = {
        pull(in)
      }
    })
  }

}
