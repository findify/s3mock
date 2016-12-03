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
        case Failure(_) => HttpResponse(StatusCodes.NotFound)
      }
    }
  }

  protected def metadataToHeaderList(metadata: ObjectMetadata): List[RawHeader] = {
    val headerList = scala.collection.mutable.ListBuffer.empty[RawHeader]

    val rawMetadata: mutable.Map[String, AnyRef] = metadata.getRawMetadata.asScala
    if (rawMetadata != null) {
      import scala.collection.JavaConversions._
      for (entry <- rawMetadata.entrySet) {
        headerList.append(RawHeader(entry.getKey, entry.getValue.toString))
      }
    }

    val httpExpiresDate: Date = metadata.getHttpExpiresDate
    if (httpExpiresDate != null) headerList.append(RawHeader(Headers.EXPIRES, DateUtils.formatRFC822Date(httpExpiresDate)))
    val userMetadata: mutable.Map[String, String] = metadata.getUserMetadata.asScala
    if (userMetadata != null) {
      import scala.collection.JavaConversions._
      for (entry <- userMetadata.entrySet) {
        var key: String = entry.getKey
        var value: String = entry.getValue
        if (key != null) key = key.trim
        if (value != null) value = value.trim
        headerList.append(RawHeader(Headers.S3_USER_METADATA_PREFIX + key, value))
      }
    }
    headerList.toList
  }
}
