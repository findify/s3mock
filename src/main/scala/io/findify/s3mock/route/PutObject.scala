package io.findify.s3mock.route

import java.lang.Iterable
import java.util

import akka.http.javadsl.model.HttpHeader
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import com.amazonaws.AmazonClientException
import com.amazonaws.services.s3.Headers
import com.amazonaws.services.s3.internal.ServiceUtils
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.util.{DateUtils, StringUtils}
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.S3ChunkedProtocolStage
import io.findify.s3mock.error.{InternalErrorException, NoSuchBucketException}
import io.findify.s3mock.provider.Provider
import org.apache.commons.codec.digest.DigestUtils

import scala.collection.JavaConverters._
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

  private def populateObjectMetadata(request: HttpRequest, bytes: Array[Byte]): ObjectMetadata = {
    val metadata = new ObjectMetadata()
    val ignoredHeaders: util.HashSet[String] = new util.HashSet[String]()
    ignoredHeaders.add(Headers.DATE)
    ignoredHeaders.add(Headers.SERVER)
    ignoredHeaders.add(Headers.REQUEST_ID)
    ignoredHeaders.add(Headers.EXTENDED_REQUEST_ID)
    ignoredHeaders.add(Headers.CLOUD_FRONT_ID)
    ignoredHeaders.add(Headers.CONNECTION)

    val headers: Iterable[HttpHeader] = request.getHeaders()
    for (header <- headers.asScala) {
      var key: String = header.name()
      if (StringUtils.beginsWithIgnoreCase(key, Headers.S3_USER_METADATA_PREFIX)) {
        key = key.substring(Headers.S3_USER_METADATA_PREFIX.length)
        metadata.addUserMetadata(key, header.value())
      }
      //      else if (ignoredHeaders.contains(key)) {
      // ignore...
      //      }
      else if (key.equalsIgnoreCase(Headers.LAST_MODIFIED)) try
        metadata.setHeader(key, ServiceUtils.parseRfc822Date(header.value()))

      catch {
        case pe: Exception => logger.warn("Unable to parse last modified date: " + header.value(), pe)
      }
      else if (key.equalsIgnoreCase(Headers.CONTENT_LENGTH)) try
        metadata.setHeader(key, java.lang.Long.parseLong(header.value()))

      catch {
        case nfe: NumberFormatException => throw new AmazonClientException("Unable to parse content length. Header 'Content-Length' has corrupted data" + nfe.getMessage, nfe)
      }
      else if (key.equalsIgnoreCase(Headers.ETAG)) metadata.setHeader(key, ServiceUtils.removeQuotes(header.value()))
      else if (key.equalsIgnoreCase(Headers.EXPIRES)) try
        metadata.setHttpExpiresDate(DateUtils.parseRFC822Date(header.value()))

      catch {
        case pe: Exception => logger.warn("Unable to parse http expiration date: " + header.value(), pe)
      }
      //      else if (key.equalsIgnoreCase(Headers.EXPIRATION)) new ObjectExpirationHeaderHandler[ObjectMetadata]().handle(metadata, response)
      //      else if (key.equalsIgnoreCase(Headers.RESTORE)) new ObjectRestoreHeaderHandler[ObjectRestoreResult]().handle(metadata, response)
      //      else if (key.equalsIgnoreCase(Headers.REQUESTER_CHARGED_HEADER)) new S3RequesterChargedHeaderHandler[S3RequesterChargedResult]().handle(metadata, response)
      else if (key.equalsIgnoreCase(Headers.S3_PARTS_COUNT)) try
        metadata.setHeader(key, header.value().toInt)

      catch {
        case nfe: NumberFormatException => throw new AmazonClientException("Unable to parse part count. Header x-amz-mp-parts-count has corrupted data" + nfe.getMessage, nfe)
      }
      else metadata.setHeader(key, header.value())
    }

    if(metadata.getContentType == null){
      metadata.setContentType(request.entity.getContentType.toString)
    }
    metadata.getRawMetadata
    metadata.setContentMD5(DigestUtils.md5Hex(bytes))
    metadata
  }

}
