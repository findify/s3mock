package io.findify.s3mock.transfermanager

import java.io.ByteArrayInputStream

import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import io.findify.s3mock.S3MockTest

class MultipartUploadTest extends S3MockTest {
  override def behaviour(fixture: => Fixture) = {
    val s3 = fixture.client

    val partSize = 1024L
    val tm = TransferManagerBuilder
      .standard()
      .withMultipartUploadThreshold(partSize)
      .withMinimumUploadPartSize(partSize)
      .withMultipartCopyThreshold(partSize)
      .withMultipartCopyPartSize(partSize)
      .withS3Client(s3)
      .build()

    def zeroes(size: Long): (ByteArrayInputStream, ObjectMetadata) = {
      val stream = new ByteArrayInputStream(new Array[Byte](size.toInt))
      val meta = new ObjectMetadata()
      meta.setContentLength(size)
      (stream, meta)
    }

    it should "multipart-upload files with TransferManager" in {
      val (stream, meta) = zeroes(100 * partSize)
      s3.createBucket("tm1")
      val upload = tm.upload("tm1", "hello1", stream, meta)
      val result = upload.waitForUploadResult()
      result.getKey shouldBe "hello1"
    }

    it should "multipart-copy files with TransferManager" in {
      val (stream, meta) = zeroes(100 * partSize)
      s3.createBucket("tm2")
      tm.upload("tm2", "hello2", stream, meta).waitForUploadResult()
      val copy = tm.copy("tm2", "hello2", "tm2", "hello2-copy")
      val res = copy.waitForCopyResult()
      res.getSourceKey shouldBe "hello2"
      res.getDestinationKey shouldBe "hello2-copy"
    }
  }
}
