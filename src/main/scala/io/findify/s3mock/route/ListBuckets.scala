package io.findify.s3mock.route

import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.server.Directives._
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.provider.Provider

/**
  * Created by shutty on 8/19/16.
  */
case class ListBuckets()(implicit provider:Provider) extends LazyLogging {
  def route() = get {
    complete {
      logger.debug("listing all buckets")
      HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(ContentType(MediaTypes.`application/xml`, HttpCharsets.`UTF-8`), provider.listBuckets.toXML.toString)
      )
    }
  }
}
