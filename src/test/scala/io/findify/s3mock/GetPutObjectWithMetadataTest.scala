package io.findify.s3mock

import java.io.ByteArrayInputStream

import com.amazonaws.services.s3.model.{ObjectMetadata, S3Object}

import scala.jdk.CollectionConverters._

/**
  * Created by shutty on 8/10/16.
  */
class GetPutObjectWithMetadataTest extends S3MockTest {
  override def behaviour(fixture: => Fixture): Unit = {
    val s3 = fixture.client
    it should "put object with metadata" in {
      s3.createBucket("getput").getName shouldBe "getput"
      s3.listBuckets().asScala.exists(_.getName == "getput") shouldBe true

      val is = new ByteArrayInputStream("bar".getBytes("UTF-8"))
      val metadata: ObjectMetadata = new ObjectMetadata()
      metadata.setContentType("application/json")
      metadata.setUserMetadata(Map("metamaic" -> "maic").asJava)

      s3.putObject("getput", "foo", is, metadata)

      val s3Object: S3Object = s3.getObject("getput", "foo")
      val actualMetadata: ObjectMetadata = s3Object.getObjectMetadata
      actualMetadata.getContentType shouldBe "application/json"

      getContent(s3Object) shouldBe "bar"
    }

    it should "put object with metadata, but skip unvalid content-type" in {
      s3.createBucket("getput").getName shouldBe "getput"
      s3.listBuckets().asScala.exists(_.getName == "getput") shouldBe true

      val is = new ByteArrayInputStream("bar".getBytes("UTF-8"))
      val metadata: ObjectMetadata = new ObjectMetadata()
      metadata.setContentType("application")
      metadata.setUserMetadata(Map("metamaic" -> "maic").asJava)

      s3.putObject("getput", "foo", is, metadata)

      val s3Object: S3Object = s3.getObject("getput", "foo")
      val actualMetadata: ObjectMetadata = s3Object.getObjectMetadata
      actualMetadata.getContentType shouldBe "application/octet-stream"

      getContent(s3Object) shouldBe "bar"
    }
    it should "put object in subdirs with metadata, but skip unvalid content-type" in {
      s3.createBucket("getput").getName shouldBe "getput"
      s3.listBuckets().asScala.exists(_.getName == "getput") shouldBe true

      val is = new ByteArrayInputStream("bar".getBytes("UTF-8"))
      val metadata: ObjectMetadata = new ObjectMetadata()
      metadata.setContentType("application")
      metadata.setUserMetadata(Map("metamaic" -> "maic").asJava)

      s3.putObject("getput", "foo1/bar", is, metadata)

      val s3Object: S3Object = s3.getObject("getput", "foo1/bar")
      val actualMetadata: ObjectMetadata = s3Object.getObjectMetadata
      actualMetadata.getContentType shouldBe "application/octet-stream"

      getContent(s3Object) shouldBe "bar"
    }
  }
}
