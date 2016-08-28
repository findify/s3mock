package io.findify.s3mock.awscli

import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.stream.scaladsl.Sink
import akka.util.ByteString

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
    response.entity.contentLengthOption shouldBe Some(3)
  }
  it should "deal with HEAD requests" in {
    s3.createBucket("awscli-head")
    s3.putObject("awscli-head", "foo2", "bar")
    val response = Await.result(http.singleRequest(HttpRequest(method = HttpMethods.HEAD, uri = "http://127.0.0.1:8001/awscli-head/foo2")), 10.seconds)
    response.headers.find(_.is("last-modified")).map(_.value()) shouldBe Some("Thu, 01 Jan 1970 00:00:00 GMT")
    response.entity.contentLengthOption shouldBe Some(0)
    Await.result(response.entity.dataBytes.fold(ByteString(""))(_ ++ _).runWith(Sink.head), 10.seconds).utf8String shouldBe ""
  }
  it should "deal with metadata requests" in {
    s3.createBucket("awscli-head2")
    s3.putObject("awscli-head2", "foo", "bar")
    val meta = s3.getObjectMetadata("awscli-head2", "foo")
    meta.getContentLength shouldBe 3
  }
}
