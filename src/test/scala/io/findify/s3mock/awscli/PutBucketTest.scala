package io.findify.s3mock.awscli

import org.apache.pekko.http.scaladsl.model.{HttpMethods, HttpRequest}
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

/**
  * Created by shutty on 8/28/16.
  */
class PutBucketTest extends AWSCliTest {
  override def behaviour(fixture: => Fixture) = {
    val s3 = fixture.client
    val port = fixture.port
    it should "create bucket with AWS CLI" in {
      val response = Await.result(http.singleRequest(HttpRequest(method = HttpMethods.PUT, uri = s"http://127.0.0.1:$port/awscli")), 10.seconds)
      s3.listBuckets().asScala.exists(_.getName == "awscli") shouldBe true
    }
  }
}
