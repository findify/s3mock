package io.findify.s3mock.route

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.provider.Provider

import scala.util.{Failure, Success, Try}

/**
  * Created by shutty on 8/19/16.
  */
case class DeleteBucket(implicit provider:Provider) extends LazyLogging {
  def route(bucket:String) = delete {
    complete {
      Try(provider.deleteBucket(bucket)) match {
        case Success(_) =>
          logger.debug(s"DELETE bucket $bucket: ok")
          HttpResponse(StatusCodes.NoContent)
        case Failure(e) =>
          logger.error(s"DELETE bucket $bucket failed: ${e.getMessage}", e)
          HttpResponse(StatusCodes.NotFound)
      }
    }
  }
}
