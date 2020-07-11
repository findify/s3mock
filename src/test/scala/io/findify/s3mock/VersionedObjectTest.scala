package io.findify.s3mock

import java.io.ByteArrayInputStream

import com.amazonaws.services.s3.model._

import scala.jdk.CollectionConverters._

/**
  * Created by furkilic on 7/11/20.
  */

class VersionedObjectTest extends S3MockTest {
  val bucketName = "versioned"

  override def behaviour(fixture: => Fixture) = {
    val s3 = fixture.client
    val versioned = fixture.versioned
    it should "put object should generate versionId" in {
      s3.createBucket(bucketName).getName shouldBe bucketName
      s3.listBuckets().asScala.exists(_.getName == bucketName) shouldBe true
      val result = s3.putObject(bucketName, "foo", "bar")
      result should not be null
      result.getVersionId should not be null
    }
    it should "MultiPartUpload should generate versionId" in {
      s3.createBucket(bucketName)
      val init = s3.initiateMultipartUpload(new InitiateMultipartUploadRequest(bucketName, "fooM"))
      val p1 = s3.uploadPart(new UploadPartRequest().withBucketName(bucketName).withPartSize(10).withKey("fooM").withPartNumber(1).withUploadId(init.getUploadId).withInputStream(new ByteArrayInputStream("hellohello".getBytes())))
      val p2 = s3.uploadPart(new UploadPartRequest().withBucketName(bucketName).withPartSize(10).withKey("fooM").withPartNumber(2).withUploadId(init.getUploadId).withInputStream(new ByteArrayInputStream("worldworld".getBytes())))
      val result = s3.completeMultipartUpload(new CompleteMultipartUploadRequest(bucketName, "fooM", init.getUploadId, List(p1.getPartETag, p2.getPartETag).asJava))
      result should not be null
      result.getVersionId should not be null
    }
    it should "put/get handle versioning object" in {
      s3.createBucket(bucketName).getName shouldBe bucketName
      s3.listBuckets().asScala.exists(_.getName == bucketName) shouldBe true
      val putObjectResult1 = s3.putObject(bucketName, "foo", "bar")
      val result1 = getContent(s3.getObject(bucketName, "foo"))
      result1 shouldBe "bar"
      s3.putObject(bucketName, "foo", "toto")
      val result2 = getContent(s3.getObject(bucketName, "foo"))
      result2 shouldBe "toto"
      val resultVersioned = getContent(s3.getObject(new GetObjectRequest(bucketName, "foo", putObjectResult1.getVersionId)))
      resultVersioned shouldBe (if(versioned) "bar" else "toto")
    }
    it should "mulipart/get handle versioning object" in {
      s3.createBucket(bucketName)
      val init = s3.initiateMultipartUpload(new InitiateMultipartUploadRequest(bucketName, "fooM"))
      val p1 = s3.uploadPart(new UploadPartRequest().withBucketName(bucketName).withPartSize(10).withKey("fooM").withPartNumber(1).withUploadId(init.getUploadId).withInputStream(new ByteArrayInputStream("hellohello".getBytes())))
      val result1 = s3.completeMultipartUpload(new CompleteMultipartUploadRequest(bucketName, "fooM", init.getUploadId, List(p1.getPartETag).asJava))
      getContent(s3.getObject(bucketName, "fooM")) shouldBe "hellohello"
      val init1 = s3.initiateMultipartUpload(new InitiateMultipartUploadRequest(bucketName, "fooM"))
      val p2 = s3.uploadPart(new UploadPartRequest().withBucketName(bucketName).withPartSize(10).withKey("fooM").withPartNumber(2).withUploadId(init1.getUploadId).withInputStream(new ByteArrayInputStream("worldworld".getBytes())))
      s3.completeMultipartUpload(new CompleteMultipartUploadRequest(bucketName, "fooM", init1.getUploadId, List(p2.getPartETag).asJava))
      getContent(s3.getObject(bucketName, "fooM")) shouldBe "worldworld"
      getContent(s3.getObject(new GetObjectRequest(bucketName, "fooM", result1.getVersionId))) shouldBe (if(versioned) "hellohello" else "worldworld")
    }
  }

}

