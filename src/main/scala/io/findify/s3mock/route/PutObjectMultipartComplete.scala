package io.findify.s3mock.route

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.provider.Provider
import io.findify.s3mock.request.CompleteMultipartUpload

/**
  * Created by shutty on 8/20/16.
  */
case class PutObjectMultipartComplete(implicit provider:Provider) extends LazyLogging {
  def route(bucket:String, path:String) = post {
    parameter('uploadId) { uploadId =>
      entity(as[String]) { xml =>
        complete {
          logger.info(s"multipart upload completed for $bucket/$path, id = $uploadId")
          val request = CompleteMultipartUpload(scala.xml.XML.loadString(xml).head)
          val response = provider.putObjectMultipartComplete(bucket, path, uploadId, request)
          HttpResponse(StatusCodes.OK, entity = response.toXML.toString)
        }
      }
    }
  }
}
