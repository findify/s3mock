package io.findify.s3mock.route

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.provider.Provider

/**
  * Created by shutty on 8/19/16.
  */
case class ListBuckets(implicit provider:Provider) extends LazyLogging {
  def route() = get {
    complete {
      logger.debug("listing all buckets")
      HttpResponse(StatusCodes.OK, entity = provider.listBuckets.toXML.toString)
    }
  }
}
