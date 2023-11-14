package io.findify.s3mock
/**
  * Created by shutty on 8/9/16.
  */
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model._

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._
class ListBucketsTest extends S3MockTest {
  override def behaviour(fixture: => Fixture) = {
    val s3 = fixture.client
    it should "list empty buckets" in {
      s3.listBuckets().isEmpty shouldBe true
    }

    it should "have correct xml content-type for bucket list" in {
      implicit val sys = fixture.system
      implicit val mat = fixture.mat
      val response = Await.result(Http().singleRequest(HttpRequest(
        method = HttpMethods.GET,
        uri = Uri(s"http://localhost:${fixture.port}/")
      )), 5.seconds)
      response.entity.contentType shouldBe ContentType(MediaTypes.`application/xml`, HttpCharsets.`UTF-8`)
    }
  }
}
