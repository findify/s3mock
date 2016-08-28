package io.findify.s3mock.awscli

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import io.findify.s3mock.S3MockTest

/**
  * Created by shutty on 8/28/16.
  */
trait AWSCliTest extends S3MockTest {
  implicit val system = ActorSystem.create("awscli")
  implicit val mat = ActorMaterializer()
  val http = Http(system)
}
