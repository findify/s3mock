package io.findify.s3mock

/**
  * Created by shutty on 8/9/16.
  */
class ListBucketTest extends S3MockTest {
  "s3 mock" should "list bucket" in {
    s3.listObjects("foo").getObjectSummaries.isEmpty shouldBe true
  }
  it should "list nonempty bucket" in {
    true
  }
}
