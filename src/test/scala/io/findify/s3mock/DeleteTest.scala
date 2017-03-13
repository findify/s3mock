package io.findify.s3mock

import com.amazonaws.services.s3.model.AmazonS3Exception

import scala.collection.JavaConversions._
import scala.util.Try

/**
  * Created by shutty on 8/11/16.
  */
class DeleteTest extends S3MockTest {
  "bucket" should "be deleted" in {
    s3.createBucket("del")
    s3.listBuckets().exists(_.getName == "del") shouldBe true
    s3.deleteBucket("del")
    s3.listBuckets().exists(_.getName == "del") shouldBe false
  }

  it should "return 404 for non existent buckets" in {
    Try(s3.deleteBucket("nodel")).isFailure shouldBe true
  }
  "object" should "be deleted" in {
    s3.createBucket("delobj")
    s3.putObject("delobj", "somefile", "foo")
    s3.listObjects("delobj", "somefile").getObjectSummaries.exists(_.getKey == "somefile") shouldBe true
    s3.deleteObject("delobj", "somefile")
    s3.listObjects("delobj", "somefile").getObjectSummaries.exists(_.getKey == "somefile") shouldBe false
  }

  it should "return 404 for non-existent keys" in {
    Try(s3.deleteObject("nodel", "xxx")).isFailure shouldBe true
  }

  it should "produce NoSuchBucket if bucket does not exist" in {
    val exc = intercept[AmazonS3Exception] {
      s3.deleteBucket("aws-404")
    }
    exc.getStatusCode shouldBe 404
    exc.getErrorCode shouldBe "NoSuchBucket"
  }
}
