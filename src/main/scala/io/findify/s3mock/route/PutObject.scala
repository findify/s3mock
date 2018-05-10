package io.findify.s3mock.route

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import com.amazonaws.services.s3.model.ObjectMetadata
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.S3ChunkedProtocolStage
import io.findify.s3mock.error.{InternalErrorException, NoSuchBucketException}
import io.findify.s3mock.provider.Provider
import org.apache.commons.codec.digest.DigestUtils

import scala.util.{Failure, Success, Try}

/**
  * Created by shutty on 8/20/16.
  */
case class PutObject(implicit provider:Provider, mat:Materializer) extends LazyLogging {
  def route(bucket:String, path:String) = put {
    extractRequest { request =>
      headerValueByName("authorization") { auth =>
        completeSigned(bucket, path)
      } ~ completePlain(bucket, path)
    }
  } ~ post {
    completePlain(bucket, path)
  }


  def completeSigned(bucket:String, path:String) = extractRequest { request =>
    complete {

      logger.info(s"put object $bucket/$path (signed)")
      val dataBytes = request.entity.dataBytes
      val maybeChunckedStream = request.headers
        .find(_.lowercaseName() == "x-amz-content-sha256")
        .map(_.value)
        .filter(_.equals("STREAMING-AWS4-HMAC-SHA256-PAYLOAD"))
        .map(_ => dataBytes.via(new S3ChunkedProtocolStage))
      val result = maybeChunckedStream
        .getOrElse(dataBytes)
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

  private def populateObjectMetadata(request: HttpRequest, bytes: Array[Byte]): ObjectMetadata = {
    val metadata = MetadataUtil.populateObjectMetadata(request)
    metadata.setContentMD5(DigestUtils.md5Hex(bytes))
    metadata
  }

}
