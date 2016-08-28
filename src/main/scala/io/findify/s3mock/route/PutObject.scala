package io.findify.s3mock.route

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.S3ChunkedProtocolStage
import io.findify.s3mock.provider.Provider

import scala.concurrent.Future

/**
  * Created by shutty on 8/20/16.
  */
case class PutObject(implicit provider:Provider, mat:Materializer) extends LazyLogging {
  def route(bucket:String, path:String) = put {
    extractRequest { request =>
      headerValueByName("authorization") { auth =>
        completeSigned(bucket, path)
      } ~ completePlain(bucket, path)
    }
  } ~ post {
    completePlain(bucket, path)
  }


  def completeSigned(bucket:String, path:String) = extractRequest { request =>
    complete {
      logger.info(s"put object $bucket/$path (signed)")
      val result = request.entity.dataBytes
        .via(new S3ChunkedProtocolStage)
        .fold(ByteString(""))(_ ++ _)
        .map(data => {
          provider.putObject(bucket, path, data.toArray)
          HttpResponse(StatusCodes.OK)
        }).runWith(Sink.head[HttpResponse])
      result
    }
  }

  def completePlain(bucket:String, path:String) = extractRequest { request =>
    complete {
      logger.info(s"put object $bucket/$path (unsigned)")
      val result = request.entity.dataBytes
        .fold(ByteString(""))(_ ++ _)
        .map(data => {
          provider.putObject(bucket, path, data.toArray)
          HttpResponse(StatusCodes.OK)
        }).runWith(Sink.head[HttpResponse])
      result
    }
  }

}
