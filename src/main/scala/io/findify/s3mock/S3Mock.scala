package io.findify.s3mock

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.provider.{FileProvider, InMemoryProvider, Provider}
import io.findify.s3mock.route._

import scala.concurrent.Await
import scala.concurrent.duration.Duration
/**
  * Created by shutty on 8/9/16.
  */
class S3Mock(port:Int, provider:Provider)(implicit system:ActorSystem = ActorSystem.create("sqsmock")) extends LazyLogging {
  implicit val p = provider
  private var bind:Http.ServerBinding = _

  val chunkSignaturePattern = """([0-9a-fA-F]+);chunk\-signature=([a-z0-9]){64}""".r

  def start = {
    implicit val mat = ActorMaterializer()
    val http = Http(system)
    val route =
      pathPrefix(Segment) { bucket =>
        pathSingleSlash {
          concat(
            ListBucket().route(bucket),
            CreateBucket().route(bucket),
            DeleteBucket().route(bucket),
            DeleteObjects().route(bucket)
          )
        } ~ pathEnd {
          concat(
            ListBucket().route(bucket),
            CreateBucket().route(bucket),
            DeleteBucket().route(bucket)
          )
        } ~ parameterMap { params =>
          path(RemainingPath) { key =>
            concat(
              GetObject().route(bucket, key.toString(), params),
              CopyObject().route(bucket, key.toString()),
              PutObjectMultipart().route(bucket, key.toString()),
              PutObjectMultipartStart().route(bucket, key.toString()),
              PutObjectMultipartComplete().route(bucket, key.toString()),
              PutObject().route(bucket, key.toString()),
              DeleteObject().route(bucket, key.toString())
            )
          }
        }
      } ~ ListBuckets().route() ~ extractRequest { request =>
        complete {
          logger.error(s"method not implemented: ${request.method.value} ${request.uri.toString}")
          HttpResponse(status = StatusCodes.NotImplemented)
        }
      }

    bind = Await.result(http.bindAndHandle(route, "localhost", port), Duration.Inf)
    bind
  }

  def stop = Await.result(bind.unbind(), Duration.Inf)

}

object S3Mock {
  def apply(port: Int): S3Mock = new S3Mock(port, new InMemoryProvider)
  def apply(port:Int, dir:String) = new S3Mock(port, new FileProvider(dir))
  def create(port:Int) = apply(port) // Java API
  def create(port:Int, dir:String) = apply(port, dir) // Java API
}
