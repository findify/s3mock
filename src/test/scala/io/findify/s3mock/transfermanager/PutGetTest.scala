package io.findify.s3mock.transfermanager

import java.io.{ByteArrayInputStream, File, FileInputStream}

import com.amazonaws.services.s3.model.ObjectMetadata
import io.findify.s3mock.S3MockTest

import scala.io.Source

/**
  * Created by shutty on 11/23/16.
  */
class PutGetTest extends S3MockTest {
  override def behaviour(fixture: => Fixture) = {
    val s3 = fixture.client
    val tm = fixture.tm

    it should "put files with TransferManager" in {
      s3.createBucket("tm1")
      val upload = tm.upload("tm1", "hello1", new ByteArrayInputStream("hello".getBytes), new ObjectMetadata())
      val result = upload.waitForUploadResult()
      result.getKey shouldBe "hello1"
    }

    it should "download files with TransferManager" in {
      val file = File.createTempFile("hello1", ".s3mock")
      val download = tm.download("tm1", "hello1", file)
      download.waitForCompletion()
      val result = Source.fromInputStream(new FileInputStream(file), "UTF-8").mkString
      result shouldBe "hello"
    }

    it should "copy file with TransferManager" in {
      val copy = tm.copy("tm1", "hello1", "tm1", "hello2")
      val result = copy.waitForCopyResult()
      result.getDestinationKey shouldBe "hello2"
      val hello2 = s3.getObject("tm1", "hello2")
      getContent(hello2) shouldBe "hello"
    }
  }
}
