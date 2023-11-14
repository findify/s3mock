package io.findify.s3mock.route

import org.apache.pekko.http.scaladsl.model.{HttpResponse, StatusCodes}
import org.apache.pekko.http.scaladsl.server.Directives._
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.error.{InternalErrorException, NoSuchBucketException}
import io.findify.s3mock.provider.Provider

import scala.util.{Failure, Success, Try}

/**
  * Created by shutty on 8/19/16.
  */
case class DeleteBucket()(implicit provider:Provider) extends LazyLogging {
  def route(bucket:String) = delete {
    complete {
      Try(provider.deleteBucket(bucket)) match {
        case Success(_) =>
          logger.debug(s"DELETE bucket $bucket: ok")
          HttpResponse(StatusCodes.NoContent)
        case Failure(e: NoSuchBucketException) =>
          logger.error(s"DELETE bucket $bucket failed: no such bucket")
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
