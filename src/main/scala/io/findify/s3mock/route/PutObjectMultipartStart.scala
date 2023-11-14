package io.findify.s3mock.route

import java.nio.charset.StandardCharsets

import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.server.Directives._
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.error.{InternalErrorException, NoSuchBucketException}
import io.findify.s3mock.provider.Provider

import scala.util.{Failure, Success, Try}

/**
  * Created by shutty on 8/20/16.
  */
case class PutObjectMultipartStart()(implicit provider:Provider) extends LazyLogging {
  def route(bucket:String, path:String) = post {
    extractRequest { request =>
      parameter('uploads) { mp =>
        complete {
          val metadata = MetadataUtil.populateObjectMetadata(request)
          logger.info(s"multipart upload start to $bucket/$path")
          Try(provider.putObjectMultipartStart(bucket, path, metadata)) match {
            case Success(result) =>
              HttpResponse(
                StatusCodes.OK,
                entity = HttpEntity(
                  ContentTypes.`application/octet-stream`, result.toXML.toString().getBytes(StandardCharsets.UTF_8)
                )
              )
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
        }
      }
    }
  }
}
