package io.findify.s3mock.route

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.error.NoSuchKeyException
import io.findify.s3mock.provider.Provider
import io.findify.s3mock.response.CopyObjectResult
import org.joda.time.DateTime

import scala.util.{Failure, Success, Try}

/**
  * Created by shutty on 11/23/16.
  */
case class CopyObject(implicit provider: Provider) extends LazyLogging {
  def split(path: String):Option[(String,String)] = {
    path.split("/").toList match {
      case bucket :: tail => Some(bucket -> tail.mkString("/"))
      case _ => None
    }
  }
  def route(destBucket:String, destKey:String) = put {
    headerValueByName("x-amz-copy-source") { source =>
      complete {
        split(source) match {
          case Some((sourceBucket, sourceKey)) =>
            Try(provider.copyObject(sourceBucket, sourceKey, destBucket, destKey)) match {
              case Success(result) =>
                logger.info(s"copied object $sourceBucket/$sourceKey")
                HttpResponse(status = StatusCodes.OK, entity = result.toXML.toString())
              case Failure(NoSuchKeyException(_, _)) =>
                logger.info(s"cannot copy object $sourceBucket/$sourceKey: no such key")
                HttpResponse(StatusCodes.NotFound)
              case Failure(ex) =>
                logger.error(s"cannot copy object $sourceBucket/$sourceKey: $ex", ex)
                HttpResponse(StatusCodes.NotFound)
            }
          case None =>
            logger.error(s"cannot copy object $source")
            HttpResponse(StatusCodes.NotFound)
        }
      }
    }
  }
}
