package io.findify.s3mock.route

import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.server.Directives._
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.error.{InternalErrorException, NoSuchBucketException}
import io.findify.s3mock.provider.Provider

import scala.util.{Failure, Success, Try}
import scala.language.postfixOps

/**
  * Created by shutty on 8/19/16.
  */
case class ListBucket()(implicit provider:Provider) extends LazyLogging {
  def route(bucket:String) = get {
    parameter('prefix?, 'delimiter?, Symbol("max-keys")?) { (prefix, delimiter, maxkeys) =>
      complete {
        logger.info(s"listing bucket $bucket with prefix=$prefix, delimiter=$delimiter")
        Try(provider.listBucket(bucket, prefix, delimiter, maxkeys.map(_.toInt))) match {
          case Success(l) => HttpResponse(
            StatusCodes.OK,
            entity = HttpEntity(ContentType(MediaTypes.`application/xml`, HttpCharsets.`UTF-8`), l.toXML.toString)
          )
          case Failure(e: NoSuchBucketException) =>
            HttpResponse(
              StatusCodes.NotFound,
              entity = HttpEntity(ContentType(MediaTypes.`application/xml`, HttpCharsets.`UTF-8`), e.toXML.toString)
            )
          case Failure(t) =>
            HttpResponse(
              StatusCodes.InternalServerError,
              entity = HttpEntity(ContentType(MediaTypes.`application/xml`, HttpCharsets.`UTF-8`), InternalErrorException(t).toXML.toString)
            )
        }
      }
    }
  }
}
