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
class ListBucketsTest extends S3MockTest {
  "s3 mock" should "list empty buckets" in {
    s3.listBuckets().isEmpty shouldBe true
  }
}
