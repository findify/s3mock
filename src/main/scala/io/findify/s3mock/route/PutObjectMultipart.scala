package io.findify.s3mock.route

import org.apache.pekko.NotUsed
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.model.headers.ETag
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.stream.scaladsl.{Flow, Sink}
import org.apache.pekko.stream.{FlowShape, Graph, Materializer}
import org.apache.pekko.util.ByteString
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.S3ChunkedProtocolStage
import io.findify.s3mock.error.{InternalErrorException, NoSuchBucketException}
import io.findify.s3mock.provider.Provider
import org.apache.commons.codec.digest.DigestUtils

import scala.util.{Failure, Success, Try}

/**
  * Created by shutty on 8/19/16.
  */
case class PutObjectMultipart()(implicit provider: Provider, mat: Materializer) extends LazyLogging {

  type EntityDecoder = Graph[FlowShape[ByteString, ByteString], NotUsed]

  private val defaultEntityEncoder = Flow[ByteString].map(identity)

  def route(bucket: String, path: String) = parameter('partNumber, 'uploadId) { (partNumber: String, uploadId: String) =>
    put {
      logger.debug(s"put multipart object bucket=$bucket path=$path")
      headerValueByName("x-amz-decoded-content-length") { decodedLength =>
        completeRequest(bucket, path, partNumber.toInt, uploadId, new S3ChunkedProtocolStage)
      } ~ completeRequest(bucket, path, partNumber.toInt, uploadId)
    } ~ post {
      logger.debug(s"post multipart object bucket=$bucket path=$path")
      completeRequest(bucket, path, partNumber.toInt, uploadId)
    }
  }

  def completeRequest(bucket: String,
                      path: String,
                      partNumber: Int,
                      uploadId: String,
                      entityDecoder: EntityDecoder = defaultEntityEncoder) =
    extractRequest { request =>
      complete {
        val result = request.entity.dataBytes
          .via(entityDecoder)
          .fold(ByteString(""))(_ ++ _)
          .map(data => {
            Try(provider.putObjectMultipartPart(bucket, path, partNumber.toInt, uploadId, data.toArray)) match {
              case Success(()) =>
                HttpResponse(
                  StatusCodes.OK,
                  entity = HttpEntity( ContentType(MediaTypes.`application/xml`, HttpCharsets.`UTF-8`), "")
                ).withHeaders(ETag(DigestUtils.md5Hex(data.toArray)))
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

}
