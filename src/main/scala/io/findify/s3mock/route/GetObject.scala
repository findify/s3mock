package io.findify.s3mock.route

import akka.http.scaladsl.model.headers.{`Content-Length`, `Last-Modified`}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.provider.Provider

import scala.util.{Failure, Success, Try}

/**
  * Created by shutty on 8/19/16.
  */
case class GetObject(implicit provider:Provider) extends LazyLogging {
  def route(bucket:String, path:String) = get {
    complete {
      logger.debug(s"get object: bucket=$bucket, path=$path")
      Try(provider.getObject(bucket, path)) match {
        case Success(data) => HttpResponse(StatusCodes.OK, entity = HttpEntity(data), headers = List(`Last-Modified`(DateTime(1970,1,1))))
        case Failure(_) => HttpResponse(StatusCodes.NotFound)
      }
    }
  }
}
