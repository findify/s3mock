package io.findify.s3mock.route

import akka.http.scaladsl.common.{NameOptionReceptacle, NameReceptacle}
import akka.http.scaladsl.model.{
  ContentType,
  HttpCharsets,
  HttpEntity,
  HttpResponse,
  MediaTypes,
  StatusCodes
}
import akka.http.scaladsl.server.Directives.{complete, get, parameters}
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.error.{
  InternalErrorException,
  NoSuchBucketException,
  NoSuchKeyException,
  NoSuchUploadException
}
import io.findify.s3mock.provider.Provider
import akka.http.scaladsl.model.HttpEntity

import scala.util.{Failure, Success, Try}

case class ListParts()(implicit provider: Provider) extends LazyLogging {
  def route(bucket: String, path: String) = get {
    parameters(
      Symbol("uploadId"),
      new NameOptionReceptacle[Int]("part-number-marker"),
      new NameOptionReceptacle[Int]("max-parts")
    ) { (uploadId: String, marker: Option[Int], count: Option[Int]) =>
      {
        complete {
          logger.debug(s"list parts bucket=$bucket path=$path uploadId=$uploadId")
          val (status, response) = Try(
            provider.listParts(bucket, path, uploadId, marker, count)
          ) match {
            case Success(listParts) =>
              (StatusCodes.OK, listParts.toXml)
            case Failure(e) =>
              val (message, status, response) = e match {
                case e: NoSuchBucketException =>
                  ("no such bucket", StatusCodes.NotFound, e.toXML)
                case e: NoSuchUploadException =>
                  ("no such upload", StatusCodes.NotFound, e.toXML)
                case e =>
                  (
                    e.toString,
                    StatusCodes.InternalServerError,
                    InternalErrorException(e).toXML
                  )
              }
              logger.info(s"cannot list parts of upload $uploadId in $bucket/$path: $message")
              (status, response)
          }

          HttpResponse(
            status,
            entity = HttpEntity(
              ContentType(MediaTypes.`application/xml`, HttpCharsets.`UTF-8`),
              response.toString
            )
          )
        }
      }
    }
  }
}
