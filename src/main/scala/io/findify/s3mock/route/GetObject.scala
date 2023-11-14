package io.findify.s3mock.route

import java.io.StringWriter
import java.net.URLDecoder
import java.util.Date

import org.apache.pekko.http.scaladsl.model.HttpEntity.Strict
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.model.headers.{RawHeader, `Last-Modified`}
import org.apache.pekko.http.scaladsl.server.Directives._
import com.amazonaws.services.s3.Headers
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.util.DateUtils
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.error.{InternalErrorException, NoSuchBucketException, NoSuchKeyException}
import io.findify.s3mock.provider.{GetObjectData, Provider}

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

/**
  * Created by shutty on 8/19/16.
  */
case class GetObject()(implicit provider: Provider) extends LazyLogging {
  def route(bucket: String, path: String, params: Map[String, String]) = get {

    withRangeSupport {
      respondWithDefaultHeader(`Last-Modified`(DateTime(1970, 1, 1))) {
        complete {
          logger.debug(s"get object: bucket=$bucket, path=$path")

          Try(provider.getObject(bucket, path)) match {
            case Success(GetObjectData(data, metaOption)) =>
              metaOption match {
                case Some(meta) =>
                  val entity: Strict = ContentType.parse(meta.getContentType) match {
                    case Right(value) => HttpEntity(value, data)
                    case Left(error) => HttpEntity(data)
                  }

                  if (params.contains("tagging")) {
                    handleTaggingRequest(meta)
                  } else {
                    HttpResponse(
                      status = StatusCodes.OK,
                      entity = entity,
                      headers = metadataToHeaderList(meta)
                    )
                  }

                case None =>
                  HttpResponse(
                    status = StatusCodes.OK,
                    entity = HttpEntity(data),
                    headers = List()
                  )
              }
            case Failure(e: NoSuchKeyException) =>
              HttpResponse(
                StatusCodes.NotFound,
                entity = e.toXML.toString()
              )
            case Failure(e: NoSuchBucketException) =>
              HttpResponse(
                StatusCodes.NotFound,
                entity = e.toXML.toString()
              )
            case Failure(t) =>
              logger.error("Oops: ", t)
              HttpResponse(
                StatusCodes.InternalServerError,
                entity = InternalErrorException(t).toXML.toString()
              )
          }
        }
      }
    }
  }



  protected def handleTaggingRequest(meta: ObjectMetadata): HttpResponse = {
    var root = <Tagging xmlns="http://s3.amazonaws.com/doc/2006-03-01/"></Tagging>
    var tagset = <TagSet></TagSet>

    var w = new StringWriter()

    if (meta.getRawMetadata.containsKey("x-amz-tagging")){
      var doc =
        <Tagging xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
          <TagSet>
            {
            meta.getRawMetadata.get("x-amz-tagging").asInstanceOf[String].split("&").map(
              (rawTag: String) => {
                rawTag.split("=", 2).map(
                  (part: String) => URLDecoder.decode(part, "UTF-8")
                )
              }).map(
              (kv: Array[String]) =>
                <Tag>
                  <Key>{kv(0)}</Key>
                  <Value>{kv(1)}</Value>
                </Tag>)
            }
          </TagSet>
        </Tagging>


      xml.XML.write(w, doc, "UTF-8", true, null)
    } else {
      var doc = <Tagging xmlns="http://s3.amazonaws.com/doc/2006-03-01/"><TagSet></TagSet></Tagging>
      xml.XML.write(w, doc, "UTF-8", true, null)
    }

    meta.setContentType("application/xml; charset=utf-8")
    HttpResponse(
      status = StatusCodes.OK,
      entity = w.toString,
      headers = `Last-Modified`(DateTime(1970, 1, 1)) :: metadataToHeaderList(meta)
    )
  }

  val headerBlacklist = Set("content-type", "connection")
  protected def metadataToHeaderList(metadata: ObjectMetadata): List[HttpHeader] = {
    val headers = Option(metadata.getRawMetadata)
      .map(_.asScala.toMap)
      .map(_.map {
        case (_, date: Date) =>
          `Last-Modified`(DateTime(new org.joda.time.DateTime(date).getMillis))
        case (key, value) =>
          RawHeader(key, value.toString)
      }.toList)
      .toList.flatten
      .filterNot(header => headerBlacklist.contains(header.lowercaseName))

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

    headers ++ httpExpires.toList ++ userHeaders ++ Option(metadata.getContentMD5).map(md5 => RawHeader(Headers.ETAG, md5))
  }
}
