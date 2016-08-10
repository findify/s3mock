package io.findify.s3mock

import com.amazonaws.services.s3.model.PutObjectRequest

/**
  * Created by shutty on 8/9/16.
  */
class ListBucketTest extends S3MockTest {
  "s3 mock" should "list bucket" in {
    s3.listObjects("foo").getObjectSummaries.isEmpty shouldBe true
  }
}
