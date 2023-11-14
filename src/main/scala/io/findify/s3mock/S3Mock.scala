package io.findify.s3mock

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.{HttpResponse, StatusCodes}
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.provider.{FileProvider, InMemoryProvider, Provider}
import io.findify.s3mock.route._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

/**
  * Create s3mock instance, the hard mode.
  * @param port port to bind to
  * @param provider backend to use. There are currently two of them implemented, FileProvider and InMemoryProvider
  * @param system actor system to use. By default, create an own one.
  */
class S3Mock(port:Int, provider:Provider)(implicit system:ActorSystem = ActorSystem.create("s3mock")) extends LazyLogging {
  implicit val p = provider
  private var bind:Http.ServerBinding = _

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

    bind = Await.result(http.bindAndHandle(route, "0.0.0.0", port), Duration.Inf)
    logger.info(s"bound to 0.0.0.0:$port")
    bind
  }

  /**
    * Stop s3mock instance. For file-based working mode, it will not clean the mounted folder.
    * This one is also not shutting down the underlying ActorSystem
    */
  def stop: Unit = Await.result(bind.unbind(), Duration.Inf)
  /**
    * Stop s3mock instance and shutdown the underlying ActorSystem.
    */
  def shutdown: Unit = {
    import system.dispatcher
    val stopped = for {
      _ <- bind.unbind()
      _ <- Http().shutdownAllConnectionPools()
      _ <- system.terminate()
    } yield {
      ()
    }
    Await.result(stopped, Duration.Inf)
  }
}

object S3Mock {
  def apply(port: Int): S3Mock = new S3Mock(port, new InMemoryProvider)
  def apply(port:Int, dir:String) = new S3Mock(port, new FileProvider(dir))

  /**
    * Create an in-memory s3mock instance
    * @param port a port to bind to.
    * @return s3mock instance
    */
  def create(port:Int) = apply(port) // Java API
  /**
    * Create a file-based s3mock instance
    * @param port port to bind to
    * @param dir directory to mount as a collection of buckets. First-level directories will be treated as buckets, their contents - as keys.
    * @return
    */
  def create(port:Int, dir:String) = apply(port, dir) // Java API
  /**
    * Builder class for java api.
    */
  class Builder {
    private var defaultPort: Int = 8001
    private var defaultProvider: Provider = new InMemoryProvider()

    /**
      * Set port to bind to
      * @param port port number
      * @return
      */
    def withPort(port: Int): Builder = {
      defaultPort = port
      this
    }

    /**
      * Use in-memory backend.
      * @return
      */
    def withInMemoryBackend(): Builder = {
      defaultProvider = new InMemoryProvider()
      this
    }

    /**
      * Use file-based backend
      * @param path Directory to mount
      * @return
      */
    def withFileBackend(path: String): Builder = {
      defaultProvider = new FileProvider(path)
      this
    }

    /**
      * Build s3mock instance
      * @return
      */
    def build(): S3Mock = {
      new S3Mock(defaultPort, defaultProvider)
    }
  }
}

