package io.findify.s3mock.route

import java.nio.charset.StandardCharsets

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import com.amazonaws.services.s3.model.{ObjectMetadata, Tag}
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.S3ChunkedProtocolStage
import io.findify.s3mock.error.{InternalErrorException, NoSuchBucketException}
import io.findify.s3mock.provider.Provider
import org.apache.commons.codec.digest.DigestUtils

import scala.util.{Failure, Success, Try}
import scala.xml.XML

/**
  * Created by shutty on 8/20/16.
  */
case class PutObject(implicit provider:Provider, mat:Materializer) extends LazyLogging {
  def route(bucket:String, path:String) = put {
    parameter('tagging?) { (tagging) ⇒
      tagging match {
        case Some(_) ⇒ completePutTags(bucket, path)
        case None ⇒
          extractRequest { request =>
            headerValueByName("authorization") { auth =>
              completeSigned(bucket, path)
            } ~ completePlain(bucket, path)
          }
      }
    }
  } ~ post {
    parameter('tagging?) { (tagging) ⇒
      tagging match {
        case Some(_) ⇒ completePutTags(bucket, path)
        case None ⇒ completePlain(bucket, path)
      }
    }
  }


  def completeSigned(bucket:String, path:String) = extractRequest { request =>
    complete {


      logger.info(s"put object $bucket/$path (signed)")
      val result = request.entity.dataBytes
        .via(new S3ChunkedProtocolStage)
        .fold(ByteString(""))(_ ++ _)
        .map(data => {
          val bytes = data.toArray
          val metadata = populateObjectMetadata(request, bytes)
          Try(provider.putObject(bucket, path, bytes, metadata)) match {
            case Success(()) => HttpResponse(StatusCodes.OK)
            case Failure(e: NoSuchBucketException) =>
              HttpResponse(
                StatusCodes.NotFound,
                entity = e.toXML.toString()
              )
            case Failure(t) =>
              HttpResponse(
                StatusCodes.InternalServerError,
                entity = InternalErrorException(t).toXML.toString()
              )
          }
        }).runWith(Sink.head[HttpResponse])
      result
    }
  }

  def completePlain(bucket:String, path:String) = extractRequest { request =>
    complete {

      logger.info(s"put object $bucket/$path (unsigned)")
      val result = request.entity.dataBytes
        .fold(ByteString(""))(_ ++ _)
        .map(data => {
          val bytes = data.toArray
          val metadata = populateObjectMetadata(request, bytes)
          Try(provider.putObject(bucket, path, bytes, metadata)) match {
            case Success(()) => HttpResponse(StatusCodes.OK)
            case Failure(e: NoSuchBucketException) =>
              HttpResponse(
                StatusCodes.NotFound,
                entity = e.toXML.toString()
              )
            case Failure(t) =>
              HttpResponse(
                StatusCodes.InternalServerError,
                entity = InternalErrorException(t).toXML.toString()
              )
          }
        }).runWith(Sink.head[HttpResponse])
      result
    }
  }

  def completePutTags(bucket:String, path:String) = extractRequest { request ⇒
    complete {
      logger.info(s"put object $bucket/$path (unsigned)")
      val result = request.entity.dataBytes
        .fold(ByteString(""))(_ ++ _)
        .map(data => {

          val tags = (for {
            tag ← XML.loadString(new String(data.toArray, StandardCharsets.UTF_8)) \ "TagSet" \ "Tag"
            key ← tag \ "Key"
            value ← tag \ "Value"
          } yield new Tag(key.text, value.text)).toList

          Try(provider.setObjectTagging(bucket, path, tags)) match {
            case Success(()) => HttpResponse(StatusCodes.OK)
            case Failure(e: NoSuchBucketException) =>
              HttpResponse(
                StatusCodes.NotFound,
                entity = e.toXML.toString()
              )
            case Failure(t) =>
              HttpResponse(
                StatusCodes.InternalServerError,
                entity = InternalErrorException(t).toXML.toString()
              )
          }

        }).runWith(Sink.head[HttpResponse])
      result
    }
  }

  private def populateObjectMetadata(request: HttpRequest, bytes: Array[Byte]): ObjectMetadata = {
    val metadata = MetadataUtil.populateObjectMetadata(request)
    metadata.setContentMD5(DigestUtils.md5Hex(bytes))
    metadata
  }

}
