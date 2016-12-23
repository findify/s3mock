package io.findify.s3mock.route

import java.util.Date

import akka.http.scaladsl.model.HttpEntity.Strict
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{RawHeader, `Last-Modified`}
import akka.http.scaladsl.server.Directives._
import com.amazonaws.services.s3.Headers
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.util.DateUtils
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.error.NoSuchKeyException
import io.findify.s3mock.provider.Provider

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.{Failure, Success, Try}

/**
  * Created by shutty on 8/19/16.
  */
case class GetObject(implicit provider: Provider) extends LazyLogging {
  def route(bucket: String, path: String) = get {
    complete {
      logger.debug(s"get object: bucket=$bucket, path=$path")

      Try(provider.getObject(bucket, path)) match {
        case Success(data) =>
          provider.getMetaData(bucket, path) match {
            case Some(meta) =>
              val entity: Strict = ContentType.parse(meta.getContentType) match {
                case Right(value) => HttpEntity(value, data)
                case Left(error) => HttpEntity(data)
              }

              HttpResponse(
                status = StatusCodes.OK,
                entity = entity,
                headers = `Last-Modified`(DateTime(1970, 1, 1)) :: metadataToHeaderList(meta)
              )
            case None =>
              HttpResponse(
                status = StatusCodes.OK,
                entity = HttpEntity(data),
                headers = List(`Last-Modified`(DateTime(1970, 1, 1)))
              )
          }
        case Failure(e: NoSuchKeyException) =>
          HttpResponse(
            StatusCodes.NotFound,
            entity = e.toXML.toString()
          )
        case Failure(_) => HttpResponse(StatusCodes.NotFound)
      }
    }
  }

  protected def metadataToHeaderList(metadata: ObjectMetadata): List[RawHeader] = {
    val headers = Option(metadata.getRawMetadata)
      .map(_.asScala.toMap)
      .map(_.map { case (key, value) => RawHeader(key, value.toString)}.toList)
      .toList.flatten

    val httpExpires = Option(metadata.getHttpExpiresDate).map(date => RawHeader(Headers.EXPIRES, DateUtils.formatRFC822Date(date)))

    val userHeaders = Option(metadata.getUserMetadata)
      .map(_.asScala.toMap)
      .map(_.map { case (key, value) => {
        val name = Option(key).map(_.trim).getOrElse("")
        val hvalue = Option(value).map(_.trim).getOrElse("")
        RawHeader(Headers.S3_USER_METADATA_PREFIX + name, hvalue)
      }}.toList)
      .toList
      .flatten

    headers ++ httpExpires.toList ++ userHeaders
  }
}
