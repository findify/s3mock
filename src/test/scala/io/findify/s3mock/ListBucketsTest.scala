package io.findify.s3mock
/**
  * Created by shutty on 8/9/16.
  */
class ListBucketsTest extends S3MockTest {
  override def behaviour(fixture: => Fixture) = {
    val s3 = fixture.client
    it should "list empty buckets" in {
      s3.listBuckets().isEmpty shouldBe true
    }
  }
}
