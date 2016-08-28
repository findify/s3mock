package io.findify.s3mock.route

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.provider.Provider

/**
  * Created by shutty on 8/19/16.
  */
case class ListBucket(implicit provider:Provider) extends LazyLogging {
  def route(bucket:String) = get {
    parameter('prefix) { prefix =>
      complete {
        logger.info(s"listing bucket $bucket with prefix=$prefix")
        HttpResponse(StatusCodes.OK, entity = provider.listBucket(bucket, prefix).toXML.toString)
      }
    } ~ complete {
      logger.info(s"listing bucket $bucket")
      HttpResponse(StatusCodes.OK, entity = provider.listBucket(bucket, "").toXML.toString)
    }
  }
}
