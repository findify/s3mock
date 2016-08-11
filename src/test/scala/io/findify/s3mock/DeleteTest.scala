package io.findify.s3mock

import scala.collection.JavaConversions._

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
  "object" should "be deleted" in {
    s3.createBucket("delobj")
    s3.putObject("delobj", "somefile", "foo")
    s3.listObjects("delobj", "somefile").getObjectSummaries.exists(_.getKey == "somefile") shouldBe true
    s3.deleteObject("delobj", "somefile")
    s3.listObjects("delobj", "somefile").getObjectSummaries.exists(_.getKey == "somefile") shouldBe false
  }
}
