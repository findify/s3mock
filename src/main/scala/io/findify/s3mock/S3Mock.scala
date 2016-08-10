package io.findify.s3mock

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.http.scaladsl._
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model.{HttpHeader, HttpResponse, Multipart, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.provider.Provider
import io.findify.s3mock.request.CreateBucketConfiguration

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.io.Source
/**
  * Created by shutty on 8/9/16.
  */
class S3Mock(port:Int, provider:Provider)(implicit system:ActorSystem = ActorSystem.create("sqsmock")) extends LazyLogging {
  private var bind:Http.ServerBinding = _

  val chunkSignaturePattern = """([\d]+);chunk\-signature=([a-z0-9]){64}""".r

  def start = {
    implicit val mat = ActorMaterializer()
    val http = Http(system)
    val route = logRequest("request", Logging.InfoLevel) {
      pathPrefix(Segment) { bucket =>
        pathSingleSlash {
          get {
            formFieldMap { opts =>
              complete {
                HttpResponse(StatusCodes.OK, entity = provider.listBucket(bucket, opts.getOrElse("prefix", "")).toXML.toString)
              }
            }
          } ~ put {
            entity(as[String]) { xml =>
              complete {
                val conf = if (xml.isEmpty) new CreateBucketConfiguration(None) else CreateBucketConfiguration(scala.xml.XML.loadString(xml).head)
                val result = provider.createBucket(bucket, conf)
                HttpResponse(StatusCodes.OK).withHeaders(Location(s"/${result.name}"))
              }
            }
          }
        } ~ path(RemainingPath) { key =>
          get {
            complete {
              HttpResponse(StatusCodes.OK, entity = provider.getObject(bucket, key.toString()))
            }
          } ~ put {
            extractRequest { request =>
              complete {
                val result:Future[HttpResponse] = request.entity.dataBytes
                  .fold(ByteString(""))( (buffer, part) => {
                    val next = Source.fromString(part.utf8String).getLines().flatMap {
                      case chunkSignaturePattern(size, sig) => None
                      case line => Some(line)
                    }.mkString
                    buffer ++ ByteString(next)
                  })
                  .map(data => {
                    provider.putObject(bucket, key.toString(), data.utf8String)
                    HttpResponse(StatusCodes.OK)
                  }).runWith(Sink.head[HttpResponse])
                result
              }
            }
          } ~ post {
            entity(as[String]) { data =>
              complete {
                provider.putObject(bucket, key.toString, data)
                HttpResponse(StatusCodes.OK)
              }
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
