package io.findify.s3mock

import java.io.ByteArrayInputStream

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.{HttpMethods, HttpRequest}
import org.apache.pekko.stream.ActorMaterializer
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.util.ByteString
import com.amazonaws.services.s3.model._
import org.apache.commons.codec.digest.DigestUtils

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

class MultipartCopyTest extends S3MockTest {
  override def behaviour(fixture: => Fixture) = {
    implicit val system = ActorSystem.create("test")
    implicit val mat = ActorMaterializer()
    val http = Http(system)
    val s3 = fixture.client
    val port = fixture.port

    it should "upload copy multipart files" in {
      s3.createBucket("source").getName shouldBe "source"
      s3.listBuckets().asScala.exists(_.getName == "source") shouldBe true
      val objectSize = 10000000;
      val blob = Random.alphanumeric.take(objectSize).mkString

      s3.putObject("source", "foo", blob)
      getContent(s3.getObject("source", "foo")) shouldBe blob

      s3.createBucket("dest").getName shouldBe "dest"

      val init = s3.initiateMultipartUpload(new InitiateMultipartUploadRequest("dest", "bar"))
      val partSize = 2500000;
      val blobs = for (i <- 0 to 3) yield {
        val blob1 = new Array[Byte](partSize)
        val bytePosition = i * partSize;
        val lastbyte = if (bytePosition + partSize - 1 >= objectSize) objectSize - 1
        else bytePosition + partSize - 1

        Random.nextBytes(blob1)
        val p1 = s3.copyPart(new CopyPartRequest().withSourceBucketName("source").withSourceKey("foo").withDestinationBucketName("dest").withDestinationKey("bar")
            .withUploadId(init.getUploadId).withFirstByte(bytePosition.toLong).withLastByte(lastbyte.toLong).withPartNumber(i))
        blob1 -> p1.getPartETag
      }
      val result = s3.completeMultipartUpload(new CompleteMultipartUploadRequest("dest", "bar", init.getUploadId, blobs.map(_._2).asJava))
      result.getKey shouldBe "bar"
      println(result.getLocation)
      val source = getContent(s3.getObject("source", "foo"))
      val dest = getContent(s3.getObject("dest", "bar"))
      dest.length() shouldBe source.length()
      DigestUtils.md5Hex(dest) shouldBe  DigestUtils.md5Hex(source)
    }


    it should "produce NoSuchBucket if bucket does not exist" in {
      val objectSize = 10000000;
      val blob = Random.alphanumeric.take(objectSize).mkString
      val exc = intercept[AmazonS3Exception] {
        val init = s3.initiateMultipartUpload(new InitiateMultipartUploadRequest("aws-404", "foo4"))
        val partSize = 2500000;
        val blobs = for (i <- 0 to 3) yield {
          val blob1 = new Array[Byte](partSize)
          val bytePosition = i * partSize;
          val lastbyte = if (bytePosition + partSize - 1 >= objectSize) objectSize - 1
          else bytePosition + partSize - 1

          Random.nextBytes(blob1)
          val p1 = s3.copyPart(new CopyPartRequest().withSourceBucketName("source").withSourceKey("foo").withDestinationBucketName("dest").withDestinationKey("bar")
            .withUploadId(init.getUploadId).withFirstByte(bytePosition.toLong).withLastByte(lastbyte.toLong).withPartNumber(i))
          blob1 -> p1.getPartETag
        }

      }
      exc.getStatusCode shouldBe 404
      exc.getErrorCode shouldBe "NoSuchBucket"
    }
  }
}
