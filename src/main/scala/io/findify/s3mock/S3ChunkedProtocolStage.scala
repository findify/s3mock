package io.findify.s3mock

import akka.stream._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.util.ByteString

import scala.util.matching.Regex

/**
  * Created by shutty on 8/11/16.
  */
case class Header(chunkSize:Int, headerSize:Int, sig:String)

class ChunkBuffer {
  val hexChars = "0123456789abcdef".getBytes.toSet
  var size = -1
  var buffer = ByteString("")
  def addChunk(data:ByteString) = buffer = buffer ++ data
  def readHeader:Option[Header] = {
    val headerBuffer = buffer.take(90)
    val size = headerBuffer.takeWhile(hexChars.contains)
    val sig = headerBuffer.drop(size.length).take(83)
    if ((size.length <= 8) && (sig.length == 83) && sig.startsWith(";chunk-signature=") && sig.endsWith("\r\n"))
      Some(Header(Integer.parseInt(size.utf8String, 16), size.length+83, sig.drop(17).dropRight(2).utf8String))
    else
      None
  }
  def pullChunk(header:Header):Option[ByteString] = {
    if (buffer.length >= header.headerSize + header.chunkSize + 2) {
      buffer = buffer.drop(header.headerSize)
      val chunk = buffer.take(header.chunkSize)
      buffer = buffer.drop(header.chunkSize + 2)
      Some(chunk)
    } else None
  }
}

class S3ChunkedProtocolStage extends GraphStage[FlowShape[ByteString,ByteString]] {
  val out = Outlet[ByteString]("s3.out")
  val in = Inlet[ByteString]("s3.in")
  override val shape = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {
    val buffer = new ChunkBuffer()

    def tryRead() = {
      buffer.readHeader match {
        case Some(header) => buffer.pullChunk(header) match {
          case Some(chunk) => push(out, chunk)
          case None => pull(in)
        }
        case None => pull(in)
      }
    }

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
