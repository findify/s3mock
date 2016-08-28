package io.findify.s3mock.route

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.S3ChunkedProtocolStage
import io.findify.s3mock.provider.Provider

import scala.concurrent.Future

/**
  * Created by shutty on 8/19/16.
  */
case class PutObjectMultipart(implicit provider:Provider, mat:Materializer) extends LazyLogging {
  def route(bucket:String, path:String) = parameter('partNumber, 'uploadId) { (partNumber:String, uploadId:String) =>
    put {
      logger.debug(s"put multipart object bucket=$bucket path=$path")
      headerValueByName("authorization") { auth =>
        completeSigned(bucket, path, partNumber.toInt, uploadId)
      } ~ completePlain(bucket, path, partNumber.toInt, uploadId)
    } ~ post {
      logger.debug(s"post multipart object bucket=$bucket path=$path")
      completePlain(bucket, path, partNumber.toInt, uploadId)
    }
  }

  def completePlain(bucket:String, path:String, partNumber:Int, uploadId:String) = extractRequest { request =>
    complete {
      val result = request.entity.dataBytes
        .fold(ByteString(""))(_ ++ _)
        .map(data => {
          provider.putObjectMultipartPart(bucket, path, partNumber.toInt, uploadId, data.toArray)
          HttpResponse(StatusCodes.OK)
        }).runWith(Sink.head[HttpResponse])
      result
    }
  }

  def completeSigned(bucket:String, path:String, partNumber:Int, uploadId:String) = extractRequest { request =>
    complete {
      val result = request.entity.dataBytes
        .via(new S3ChunkedProtocolStage)
        .fold(ByteString(""))(_ ++ _)
        .map(data => {
          provider.putObjectMultipartPart(bucket, path, partNumber.toInt, uploadId, data.toArray)
          HttpResponse(StatusCodes.OK)
        }).runWith(Sink.head[HttpResponse])
      result
    }
  }

}
