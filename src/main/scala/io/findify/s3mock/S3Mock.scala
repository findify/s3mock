package io.findify.s3mock

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.http.scaladsl._
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.provider.Provider

import scala.concurrent.Await
import scala.concurrent.duration.Duration
/**
  * Created by shutty on 8/9/16.
  */
class S3Mock(port:Int, provider:Provider)(implicit system:ActorSystem = ActorSystem.create("sqsmock")) extends LazyLogging {
  private var bind:Http.ServerBinding = _
  def start = {
    implicit val mat = ActorMaterializer()
    val http = Http(system)
    val route = logRequest("request", Logging.InfoLevel) {
      pathPrefix(Segment) { bucket =>
        get {
          formFieldMap { options =>
            complete {
              "ok"
            }
          }
        }
      } ~ get {
          complete {
            HttpResponse(StatusCodes.OK, entity = provider.listBuckets.toXML.toString)
          }
      }
    }

    bind = Await.result(http.bindAndHandle(route, "localhost", port), Duration.Inf)
    bind
  }

  def stop = Await.result(bind.unbind(), Duration.Inf)

}
