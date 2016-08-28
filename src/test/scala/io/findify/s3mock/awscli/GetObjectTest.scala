package io.findify.s3mock.awscli

import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.collection.JavaConversions._

/**
  * Created by shutty on 8/28/16.
  */
class GetObjectTest extends AWSCliTest {
  "awscli cp" should "receive LastModified header" in {
    s3.createBucket("awscli-lm")
    s3.putObject("awscli-lm", "foo", "bar")
    val response = Await.result(http.singleRequest(HttpRequest(method = HttpMethods.GET, uri = "http://127.0.0.1:8001/awscli-lm/foo")), 10.seconds)
    response.headers.find(_.is("last-modified")).map(_.value()) shouldBe Some("Thu, 01 Jan 1970 00:00:00 GMT")
  }
}
