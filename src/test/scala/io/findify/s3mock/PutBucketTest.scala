package io.findify.s3mock
import scala.jdk.CollectionConverters._
/**
  * Created by shutty on 8/10/16.
  */
class PutBucketTest extends S3MockTest {
  override def behaviour(fixture: => Fixture) = {
    val s3 = fixture.client
    it should "create buckets" in {
      s3.listBuckets().isEmpty shouldBe true
      s3.createBucket("hello").getName shouldBe "hello"
      s3.listBuckets().asScala.exists(_.getName == "hello") shouldBe true
    }
    it should "create buckets with region" in {
      s3.createBucket("hello2", "us-west-1")
      s3.listBuckets().asScala.exists(_.getName == "hello2") shouldBe true
    }
  }
}
