package io.findify.s3mock.transfermanager

import java.io.{ByteArrayInputStream, File, FileInputStream}
import java.nio.charset.Charset

import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import io.findify.s3mock.S3MockTest
import org.apache.commons.io.IOUtils

/**
  * Created by shutty on 11/23/16.
  */
class PutGetTest extends S3MockTest {
  val tm = TransferManagerBuilder.standard().withS3Client(s3).build()

  override def afterAll = {
    tm.shutdownNow()
    super.afterAll
  }

  "transfer manager" should "put files" in {
    s3.createBucket("tm1")
    val upload = tm.upload("tm1", "hello1", new ByteArrayInputStream("hello".getBytes), new ObjectMetadata())
    val result = upload.waitForUploadResult()
    result.getKey shouldBe "hello1"
  }

  it should "download files" in {
    val file = File.createTempFile("hello1", ".s3mock")
    val download = tm.download("tm1", "hello1", file)
    download.waitForCompletion()
    val result = IOUtils.toString(new FileInputStream(file), Charset.forName("UTF-8"))
    result shouldBe "hello"
  }

  it should "copy file" in {
    val copy = tm.copy("tm1", "hello1", "tm1", "hello2")
    val result = copy.waitForCopyResult()
    result.getDestinationKey shouldBe "hello2"
    val hello2 = s3.getObject("tm1", "hello2")
    IOUtils.toString(hello2.getObjectContent, Charset.forName("UTF-8")) shouldBe "hello"
  }
}
