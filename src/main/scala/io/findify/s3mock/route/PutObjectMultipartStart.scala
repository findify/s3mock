package io.findify.s3mock.route

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.provider.Provider

/**
  * Created by shutty on 8/20/16.
  */
case class PutObjectMultipartStart(implicit provider:Provider) extends LazyLogging {
  def route(bucket:String, path:String) = post {
    parameter('uploads) { mp =>
      complete {
        logger.info(s"multipart upload start to $bucket/$path")
        HttpResponse(StatusCodes.OK, entity = provider.putObjectMultipartStart(bucket, path).toXML.toString())
      }
    }
  }
}
