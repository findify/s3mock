package io.findify.s3mock

import java.io.ByteArrayInputStream
import java.util

import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest}

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

  it should "bne copied with metadata" in {
    s3.createBucket("bucket-3")
    val meta = new ObjectMetadata()
    val user = new util.HashMap[String,String]()
    user.put("a", "b")
    meta.setUserMetadata(user)
    val req = new PutObjectRequest("bucket-3", "test.txt", new ByteArrayInputStream(Array(61.toByte, 62.toByte, 63.toByte)), meta)
    s3.putObject(req)
    s3.copyObject("bucket-3", "test.txt", "bucket-3", "test2.txt")
    val obj = s3.getObject("bucket-3", "test2.txt")
    obj.getObjectMetadata.getUserMetadata.get("a") shouldBe "b"
  }
}
