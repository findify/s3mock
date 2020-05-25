package io.findify.s3mock

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import scala.jdk.CollectionConverters._
import scala.io.Source

/**
  * Created by shutty on 8/19/16.
  */
class JavaExampleTest extends S3MockTest {
  override def behaviour(fixture: => Fixture) = {
    val s3 = fixture.client
    val port = fixture.port
    it should "upload files with anonymous credentials" in {
      s3.createBucket("getput").getName shouldBe "getput"
      s3.listBuckets().asScala.exists(_.getName == "getput") shouldBe true
      s3.putObject("getput", "foo", "bar")
      val result = Source.fromInputStream(s3.getObject("getput", "foo").getObjectContent, "UTF-8").mkString
      result shouldBe "bar"
    }

    it should "upload files with basic credentials" in {
      val s3b = new AmazonS3Client(new BasicAWSCredentials("foo", "bar"))
      s3b.setEndpoint(s"http://127.0.0.1:$port")
      s3b.putObject("getput", "foo2", "bar2")
      val result = Source.fromInputStream(s3b.getObject("getput", "foo2").getObjectContent, "UTF-8").mkString
      result shouldBe "bar2"

    }
  }
}

