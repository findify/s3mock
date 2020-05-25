package io.findify.s3mock

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util

import com.amazonaws.services.s3.model.{CopyObjectRequest, ObjectMetadata, PutObjectRequest}

/**
  * Created by shutty on 3/13/17.
  */
class CopyObjectTest extends S3MockTest {
  override def behaviour(fixture: => Fixture) = {
    val s3 = fixture.client
    it should "copy an object even if destdir does not exist" in {
      s3.createBucket("bucket-1")
      s3.createBucket("bucket-2")
      s3.putObject("bucket-1", "test.txt", "contents")
      s3.copyObject("bucket-1", "test.txt", "bucket-2", "folder/test.txt")
      getContent(s3.getObject("bucket-2", "folder/test.txt")) shouldBe "contents"
    }

    it should "copy an object with metadata" in {
      s3.createBucket("bucket-3")
      val meta = new ObjectMetadata()
      val user = new util.HashMap[String, String]()
      user.put("a", "b")
      meta.setUserMetadata(user)
      val req = new PutObjectRequest("bucket-3", "test.txt", new ByteArrayInputStream(Array(61.toByte, 62.toByte, 63.toByte)), meta)
      s3.putObject(req)
      s3.copyObject("bucket-3", "test.txt", "bucket-3", "test2.txt")
      val obj = s3.getObject("bucket-3", "test2.txt")
      obj.getObjectMetadata.getUserMetadata.get("a") shouldBe "b"
    }

    it should "copy an object with new metadata" in {
      s3.createBucket("test-bucket")

      val meta = new ObjectMetadata
      meta.addUserMetadata("key1", "value1")
      meta.addUserMetadata("key2", "value2")
      val putRequest = new PutObjectRequest("test-bucket", "test.txt", new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8)), meta)
      s3.putObject(putRequest)

      val newMeta = new ObjectMetadata
      newMeta.addUserMetadata("new-key1", "new-value1")
      newMeta.addUserMetadata("new-key2", "new-value2")
      val copyRequest = new CopyObjectRequest("test-bucket", "test.txt", "test-bucket", "test2.txt").withNewObjectMetadata(newMeta)
      s3.copyObject(copyRequest)

      val obj = s3.getObject("test-bucket", "test2.txt")
      obj.getObjectMetadata.getUserMetadata.size shouldBe 2
      obj.getObjectMetadata.getUserMetadata.get("new-key1") shouldBe "new-value1"
      obj.getObjectMetadata.getUserMetadata.get("new-key2") shouldBe "new-value2"
    }

    it should "copy an object with = in key" in {
      s3.createBucket("test-bucket")
      s3.putObject("test-bucket", "path/with=123/test.txt", "contents")

      val copyRequest = new CopyObjectRequest("test-bucket", "path/with=123/test.txt", "test-bucket", "path/with=345/test2.txt")
      s3.copyObject(copyRequest)
      getContent(s3.getObject("test-bucket", "path/with=345/test2.txt")) shouldBe "contents"
    }
  }
}
