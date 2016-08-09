package io.findify.s3mock

import java.util.UUID

import better.files.File
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import io.findify.s3mock.provider.FileProvider
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpec, Matchers}

import scala.collection.JavaConversions._
/**
  * Created by shutty on 8/9/16.
  */
class ListBucketsTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  val s3 = new AmazonS3Client(new BasicAWSCredentials("hello", "world"))
  s3.setEndpoint("http://localhost:8001")

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

  "s3 mock" should "list empty buckets" in {
    s3.listBuckets().isEmpty shouldBe true
  }
}
