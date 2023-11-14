package io.findify.s3mock.route

import java.net.URLDecoder
import java.util

import org.apache.pekko.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import org.apache.pekko.http.scaladsl.server.Directives._
import com.amazonaws.services.s3.model.ObjectMetadata
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.error.{InternalErrorException, NoSuchBucketException, NoSuchKeyException}
import io.findify.s3mock.provider.Provider

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

/**
  * Created by shutty on 11/23/16.
  */
case class CopyObject()(implicit provider: Provider) extends LazyLogging {
  def split(path: String):Option[(String,String)] = {
    val noFirstSlash = path.replaceAll("^/+", "")
    val result = noFirstSlash.split("/").toList match {
      case bucket :: tail => Some(bucket -> tail.mkString("/"))
      case _ => None
    }
    result
  }

  def extractMetadata(req: HttpRequest): Option[ObjectMetadata] = {
    req.headers.find(_.lowercaseName() == "x-amz-metadata-directive").map(_.value()) match {
      case Some("REPLACE") =>
        val user = new util.HashMap[String,String]()
        req.headers.filter(_.name().startsWith("x-amz-meta-")).map(h => h.name().replaceAll("x-amz-meta-", "") -> h.value()).foreach { case (k,v) => user.put(k,v) }
        val contentType = req.entity.contentType.value
        val meta = new ObjectMetadata()
        meta.setUserMetadata(user)
        meta.setContentType(contentType)
        Some(meta)
      case Some("COPY") | None => None
    }
  }
  def route(destBucket:String, destKey:String) = put {
    headerValueByName("x-amz-copy-source") { source =>
      val decodedSource = URLDecoder.decode(source, "utf-8")
      extractRequest { req =>
        complete {
          val meta = extractMetadata(req)
          split(decodedSource) match {
            case Some((sourceBucket, sourceKey)) =>
              Try(provider.copyObject(sourceBucket, sourceKey, destBucket, destKey, meta)) match {
                case Success(result) =>
                  logger.info(s"copied object $sourceBucket/$sourceKey")
                  HttpResponse(status = StatusCodes.OK, entity = result.toXML.toString())
                case Failure(e: NoSuchKeyException) =>
                  logger.info(s"cannot copy object $sourceBucket/$sourceKey: no such key")
                  HttpResponse(
                    StatusCodes.NotFound,
                    entity = e.toXML.toString()
                  )
                case Failure(e: NoSuchBucketException) =>
                  logger.info(s"cannot copy object $sourceBucket/$sourceKey: no such bucket")
                  HttpResponse(
                    StatusCodes.NotFound,
                    entity = e.toXML.toString()
                  )
                case Failure(t) =>
                  logger.error(s"cannot copy object $sourceBucket/$sourceKey: $t", t)
                  HttpResponse(
                    StatusCodes.InternalServerError,
                    entity = InternalErrorException(t).toXML.toString()
                  )
              }
            case None =>
              logger.error(s"cannot copy object $source")
              HttpResponse(StatusCodes.NotFound)
          }
        }
      }
    }
  }
}
