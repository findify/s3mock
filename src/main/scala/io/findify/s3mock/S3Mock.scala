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
  * Create s3mock instance, the hard mode.
  *
  * @param port      port to bind to
  * @param provider  backend to use. There are currently two of them implemented, FileProvider and InMemoryProvider
  * @param interface interface to bind on
  * @param system    actor system to use. By default, create an own one.
  */
class S3Mock(port: Int, provider: Provider, interface: String)(implicit system: ActorSystem = ActorSystem.create("s3mock")) extends LazyLogging {
  implicit val p = provider
  private var bind: Http.ServerBinding = _

  def start: Http.ServerBinding = {
    implicit val mat: ActorMaterializer = ActorMaterializer()
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
            DeleteBucket().route(bucket),
            DeleteObjects().route(bucket)
          )
        } ~ parameterMap { params =>
          path(RemainingPath) { key =>
            concat(
              GetObject().route(bucket, key.toString(), params),
              CopyObjectMultipart().route(bucket, key.toString()),
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

    bind = Await.result(http.bindAndHandle(route, interface, port), Duration.Inf)
    logger.info(s"bound to $interface:$port")
    bind
  }

  /**
    * Stop s3mock instance. For file-based working mode, it will not clean the mounted folder.
    * This one is also not shutting down the underlying ActorSystem
    */
  def stop(): Unit = Await.result(bind.unbind(), Duration.Inf)

  /**
    * Stop s3mock instance and shutdown the underlying ActorSystem.
    */
  def shutdown(): Unit = {
    import system.dispatcher
    val stopped = for {
      _ <- bind.unbind()
      _ <- Http().shutdownAllConnectionPools()
      _ <- system.terminate()
    } yield {
      Unit
    }
    Await.result(stopped, Duration.Inf)
  }
}

object S3Mock {
  def apply(port: Int, interface: String): S3Mock = new S3Mock(port, new InMemoryProvider, interface)

  def apply(port: Int, dir: String, interface: String): S3Mock = new S3Mock(port, new FileProvider(dir), interface)

  /**
    * Create an in-memory s3mock instance
    *
    * @param port a port to bind to.
    * @return s3mock instance
    */
  def create(port: Int, interface: String): S3Mock = apply(port, interface) // Java API
  /**
    * Create a file-based s3mock instance
    *
    * @param port port to bind to
    * @param dir  directory to mount as a collection of buckets. First-level directories will be treated as buckets, their contents - as keys.
    * @return
    */
  def create(port: Int, dir: String, interface: String) = apply(port, dir, interface) // Java API
  /**
    * Builder class for java api.
    */
  class Builder {
    private var defaultInterface: String = "0.0.0.0"
    private var defaultPort: Int = 8001
    private var defaultProvider: Provider = new InMemoryProvider()

    /**
      * Set interface to bind to
      *
      * @param interface port number
      * @return
      */
    def withInterface(interface: String): Builder = {
      defaultInterface = interface
      this
    }

    /**
      * Set port to bind to
      *
      * @param port port number
      * @return
      */
    def withPort(port: Int): Builder = {
      defaultPort = port
      this
    }

    /**
      * Use in-memory backend.
      *
      * @return
      */
    def withInMemoryBackend(): Builder = {
      defaultProvider = new InMemoryProvider()
      this
    }

    /**
      * Use file-based backend
      *
      * @param path Directory to mount
      * @return
      */
    def withFileBackend(path: String): Builder = {
      defaultProvider = new FileProvider(path)
      this
    }

    /**
      * Build s3mock instance
      *
      * @return
      */
    def build(): S3Mock = {
      new S3Mock(defaultPort, defaultProvider, defaultInterface)
    }
  }

}

