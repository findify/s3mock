package io.findify.s3mock

import java.nio.charset.Charset

import com.amazonaws.services.s3.model.{ObjectMetadata, S3Object}
import org.apache.commons.io.IOUtils

import scala.collection.JavaConversions._

/**
  * Created by shutty on 8/10/16.
  */
class GetPutObjectWithMetadataTest extends S3MockTest {
  "s3 mock" should "put object with metadata" in {
    s3.createBucket("getput").getName shouldBe "getput"
    s3.listBuckets().exists(_.getName == "getput") shouldBe true

    val is = IOUtils.toInputStream("bar", Charset.forName("UTF-8"))
    val metadata: ObjectMetadata = new ObjectMetadata()
    metadata.setContentType("application/json")
    metadata.setUserMetadata(Map("metamaic" -> "maic"))

    s3.putObject("getput", "foo", is, metadata)

    val s3Object: S3Object = s3.getObject("getput", "foo")
    val actualMetadata: ObjectMetadata = s3Object.getObjectMetadata
    actualMetadata.getContentType shouldBe "application/json"

    val result = IOUtils.toString(s3Object.getObjectContent, Charset.forName("UTF-8"))
    result shouldBe "bar"
  }

  "s3 mock" should "put object with metadata, but skip unvalid content-type" in {
    s3.createBucket("getput").getName shouldBe "getput"
    s3.listBuckets().exists(_.getName == "getput") shouldBe true

    val is = IOUtils.toInputStream("bar", Charset.forName("UTF-8"))
    val metadata: ObjectMetadata = new ObjectMetadata()
    metadata.setContentType("application")
    metadata.setUserMetadata(Map("metamaic" -> "maic"))

    s3.putObject("getput", "foo", is, metadata)

    val s3Object: S3Object = s3.getObject("getput", "foo")
    val actualMetadata: ObjectMetadata = s3Object.getObjectMetadata
    actualMetadata.getContentType shouldBe "application/octet-stream"

    val result = IOUtils.toString(s3Object.getObjectContent, Charset.forName("UTF-8"))
    result shouldBe "bar"
  }
  "s3 mock" should "put object in subdirs with metadata, but skip unvalid content-type" in {
    s3.createBucket("getput").getName shouldBe "getput"
    s3.listBuckets().exists(_.getName == "getput") shouldBe true

    val is = IOUtils.toInputStream("bar", Charset.forName("UTF-8"))
    val metadata: ObjectMetadata = new ObjectMetadata()
    metadata.setContentType("application")
    metadata.setUserMetadata(Map("metamaic" -> "maic"))

    s3.putObject("getput", "foo1/bar", is, metadata)

    val s3Object: S3Object = s3.getObject("getput", "foo1/bar")
    val actualMetadata: ObjectMetadata = s3Object.getObjectMetadata
    actualMetadata.getContentType shouldBe "application/octet-stream"

    val result = IOUtils.toString(s3Object.getObjectContent, Charset.forName("UTF-8"))
    result shouldBe "bar"
  }

}
