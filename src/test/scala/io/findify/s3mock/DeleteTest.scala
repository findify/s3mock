package io.findify.s3mock

import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model._
import com.amazonaws.services.s3.model.{AmazonS3Exception, DeleteObjectsRequest}

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.util.Try
import scala.concurrent.duration._

/**
  * Created by shutty on 8/11/16.
  */
class DeleteTest extends S3MockTest {
  override def behaviour(fixture: => Fixture) = {
    val s3 = fixture.client
    it should "delete a bucket" in {
      s3.createBucket("del")
      s3.listBuckets().asScala.exists(_.getName == "del") shouldBe true
      s3.deleteBucket("del")
      s3.listBuckets().asScala.exists(_.getName == "del") shouldBe false
    }

    it should "return 404 for non existent buckets when deleting" in {
      Try(s3.deleteBucket("nodel")).isFailure shouldBe true
    }

    it should "delete an object" in {
      s3.createBucket("delobj")
      s3.putObject("delobj", "somefile", "foo")
      s3.listObjects("delobj", "somefile").getObjectSummaries.asScala.exists(_.getKey == "somefile") shouldBe true
      s3.deleteObject("delobj", "somefile")
      s3.listObjects("delobj", "somefile").getObjectSummaries.asScala.exists(_.getKey == "somefile") shouldBe false
    }

    it should "return 404 for non-existent keys when deleting" in {
      Try(s3.deleteObject("nodel", "xxx")).isFailure shouldBe true
    }

    it should "produce NoSuchBucket if bucket does not exist when deleting" in {
      val exc = intercept[AmazonS3Exception] {
        s3.deleteBucket("aws-404")
      }
      exc.getStatusCode shouldBe 404
      exc.getErrorCode shouldBe "NoSuchBucket"
    }

    it should "delete multiple objects at once" in {
      s3.createBucket("delobj2")
      s3.putObject("delobj2", "somefile1", "foo1")
      s3.putObject("delobj2", "somefile2", "foo2")
      s3.listObjects("delobj2", "somefile").getObjectSummaries.size() shouldBe 2
      val del = s3.deleteObjects(new DeleteObjectsRequest("delobj2").withKeys("somefile1", "somefile2"))
      del.getDeletedObjects.size() shouldBe 2
      s3.listObjects("delobj2", "somefile").getObjectSummaries.size() shouldBe 0
    }

    it should "do nothing in case for deleting a subpath" in {
      s3.createBucket("delobj3")
      s3.putObject("delobj3", "some/path/foo1", "foo1")
      s3.putObject("delobj3", "some/path/foo2", "foo2")
      val del = s3.deleteObject("delobj3", "some/path")
      s3.listObjects("delobj3", "some/path/").getObjectSummaries.size() shouldBe 2
    }

    it should "work with aws sdk 2.0 style multi-object delete" in {
      implicit val mat = fixture.mat
      s3.createBucket("owntracks")
      s3.putObject("owntracks", "data/2017-07-31/10:34.json", "foo")
      s3.putObject("owntracks", "data/2017-07-31/16:23.json", "bar")
      val requestData = """<?xml version="1.0" encoding="UTF-8"?><Delete xmlns="http://s3.amazonaws.com/doc/2006-03-01/"><Object><Key>data/2017-07-31/10:34.json</Key></Object><Object><Key>data/2017-07-31/16:23.json</Key></Object></Delete>"""
      val response = Await.result(Http(fixture.system).singleRequest(HttpRequest(
        method = HttpMethods.POST,
        uri = s"http://localhost:${fixture.port}/owntracks?delete",
        entity = HttpEntity(ContentType(MediaTypes.`application/xml`, HttpCharsets.`UTF-8`), requestData)
      )), 10.seconds)
      s3.listObjects("owntracks").getObjectSummaries.isEmpty shouldBe true
    }
  }
}
