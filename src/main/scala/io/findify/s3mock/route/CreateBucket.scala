package io.findify.s3mock.route

import org.apache.pekko.http.scaladsl.model.headers.Location
import org.apache.pekko.http.scaladsl.model.{HttpResponse, StatusCodes}
import org.apache.pekko.http.scaladsl.server.Directives._
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.provider.Provider
import io.findify.s3mock.request.CreateBucketConfiguration

/**
  * Created by shutty on 8/19/16.
  */
case class CreateBucket()(implicit provider:Provider) extends LazyLogging {
  def route(bucket:String) = put {
    entity(as[String]) { xml =>
      complete {
        logger.info(s"PUT bucket $bucket")
        val conf = if (xml.isEmpty) new CreateBucketConfiguration(None) else CreateBucketConfiguration(scala.xml.XML.loadString(xml).head)
        val result = provider.createBucket(bucket, conf)
        HttpResponse(StatusCodes.OK).withHeaders(Location(s"/${result.name}"))
      }
    } ~ {
      complete {
        "ok"
      }
    }
  }
}
