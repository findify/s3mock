package io.findify.s3mock

import java.util.UUID

import scala.collection.JavaConversions._
import better.files.File
import com.amazonaws.auth.{AnonymousAWSCredentials, BasicAWSCredentials}
import com.amazonaws.services.s3.AmazonS3Client
import io.findify.s3mock.provider.FileProvider
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.io.Source

/**
  * Created by shutty on 8/19/16.
  */
class JavaExampleTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  val s3 = new AmazonS3Client(new AnonymousAWSCredentials())
  s3.setEndpoint("http://127.0.0.1:8001")

  val workDir = s"/tmp/${UUID.randomUUID()}"
  val server = new S3Mock(8001, new FileProvider(workDir))

  override def beforeAll = {
    if (!File(workDir).exists) File(workDir).createDirectory()
    server.start
  }

  override def afterAll = {
    server.stop
    File(workDir).delete()
  }

  "java example" should "upload files with anonymous credentials" in {
    s3.createBucket("getput").getName shouldBe "getput"
    s3.listBuckets().exists(_.getName == "getput") shouldBe true
    s3.putObject("getput", "foo", "bar")
    val result = Source.fromInputStream(s3.getObject("getput", "foo").getObjectContent, "UTF-8").mkString
    result shouldBe "bar"
  }

  it should "upload files with basic credentials" in {
    val s3b = new AmazonS3Client(new BasicAWSCredentials("foo", "bar"))
    s3b.setEndpoint("http://127.0.0.1:8001")
    s3b.putObject("getput", "foo2", "bar2")
    val result = Source.fromInputStream(s3b.getObject("getput", "foo2").getObjectContent, "UTF-8").mkString
    result shouldBe "bar2"

  }
}

