package io.findify.s3mock.awscli

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.stream.ActorMaterializer
import io.findify.s3mock.S3MockTest

/**
  * Created by shutty on 8/28/16.
  */
trait AWSCliTest extends S3MockTest {
  implicit val system = ActorSystem.create("awscli")
  implicit val mat = ActorMaterializer()
  val http = Http(system)
}
