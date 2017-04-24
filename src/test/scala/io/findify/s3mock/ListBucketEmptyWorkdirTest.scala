package io.findify.s3mock

import better.files.File
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import io.findify.s3mock.provider.FileProvider
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.collection.JavaConverters._

/**
  * Created by shutty on 8/30/16.
  */
class ListBucketEmptyWorkdirTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  lazy val s3 = new AmazonS3Client(new BasicAWSCredentials("hello", "world"))

  val workDir = File.newTemporaryDirectory().pathAsString
  lazy val server = new S3Mock(8001, new FileProvider(workDir))

  override def beforeAll = {
    s3.setEndpoint("http://127.0.0.1:8001")
    server.start
  }
  override def afterAll = {
    server.stop
  }

  "s3mock" should "list bucket with empty prefix" in {
    s3.createBucket("list")
    s3.putObject("list", "foo1", "xxx")
    s3.putObject("list", "foo2", "xxx")
    val list = s3.listObjects("list").getObjectSummaries.asScala.toList
    list.map(_.getKey).forall(_.startsWith("foo")) shouldBe true
  }


}
