package io.findify.s3mock

import better.files.File
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.S3Object
import io.findify.s3mock.provider.FileProvider
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.io.Source

/**
  * Created by shutty on 8/9/16.
  */
trait S3MockTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  val s3 = new AmazonS3Client(new BasicAWSCredentials("hello", "world"))
  s3.setEndpoint("http://127.0.0.1:8001")

  val workDir = File.newTemporaryDirectory().pathAsString
  val server = new S3Mock(8001, new FileProvider(workDir))

  override def beforeAll = {
    if (!File(workDir).exists) File(workDir).createDirectory()
    server.start
    super.beforeAll
  }
  override def afterAll = {
    super.afterAll
    server.stop
    File(workDir).delete()
  }

  def getContent(s3Object: S3Object): String = Source.fromInputStream(s3Object.getObjectContent, "UTF-8").mkString

}
