package io.findify.s3mock

/**
  * Created by shutty on 3/13/17.
  */
class CopyObjectTest extends S3MockTest {
  "object" should "be copied even if destdir does not exist" in {
    s3.createBucket("bucket-1")
    s3.createBucket("bucket-2")
    s3.putObject("bucket-1", "test.txt", "contents")
    s3.copyObject("bucket-1", "test.txt", "bucket-2", "folder/test.txt")
    getContent(s3.getObject("bucket-2", "folder/test.txt")) shouldBe "contents"
  }
}
