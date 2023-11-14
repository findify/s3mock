package io.findify.s3mock

import java.io.ByteArrayInputStream
import java.nio.charset.Charset

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.{HttpMethods, HttpRequest}
import org.apache.pekko.stream.ActorMaterializer
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.util.ByteString
import com.amazonaws.services.s3.model._
import org.apache.commons.codec.digest.DigestUtils

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.util.Random

/**
  * Created by shutty on 8/10/16.
  */
class MultipartUploadTest extends S3MockTest {
  override def behaviour(fixture: => Fixture) = {
    implicit val system = ActorSystem.create("test")
    implicit val mat = ActorMaterializer()
    val http = Http(system)
    val s3 = fixture.client
    val port = fixture.port

    it should "upload multipart files" in {
      s3.createBucket("getput")
      val response1 = Await.result(http.singleRequest(HttpRequest(method = HttpMethods.POST, uri = s"http://127.0.0.1:$port/getput/foo2?uploads")), 10.minutes)
      val data = Await.result(response1.entity.dataBytes.fold(ByteString(""))(_ ++ _).runWith(Sink.head), 10.seconds)
      val uploadId = (scala.xml.XML.loadString(data.utf8String) \ "UploadId").text
      val response2 = Await.result(http.singleRequest(HttpRequest(method = HttpMethods.POST, uri = s"http://127.0.0.1:$port/getput/foo2?partNumber=1&uploadId=$uploadId", entity = "foo")), 10.minutes)
      response2.status.intValue() shouldBe 200
      val response3 = Await.result(http.singleRequest(HttpRequest(method = HttpMethods.POST, uri = s"http://127.0.0.1:$port/getput/foo2?partNumber=2&uploadId=$uploadId", entity = "boo")), 10.minutes)
      response3.status.intValue() shouldBe 200
      val commit = """<CompleteMultipartUpload>
          |  <Part>
          |    <PartNumber>1</PartNumber>
          |    <ETag>ETag</ETag>
          |  </Part>
          |  <Part>
          |    <PartNumber>2</PartNumber>
          |    <ETag>ETag</ETag>
          |  </Part>
          |</CompleteMultipartUpload>""".stripMargin
      val response4 = Await.result(http.singleRequest(HttpRequest(method = HttpMethods.POST, uri = s"http://127.0.0.1:$port/getput/foo2?uploadId=$uploadId", entity = commit)), 10.minutes)
      response4.status.intValue() shouldBe 200

      getContent(s3.getObject("getput", "foo2")) shouldBe "fooboo"
    }

    it should "work with java sdk" in {
      s3.createBucket("getput")
      val init = s3.initiateMultipartUpload(new InitiateMultipartUploadRequest("getput", "foo4"))
      val p1 = s3.uploadPart(new UploadPartRequest().withBucketName("getput").withPartSize(10).withKey("foo4").withPartNumber(1).withUploadId(init.getUploadId).withInputStream(new ByteArrayInputStream("hellohello".getBytes())))
      val p2 = s3.uploadPart(new UploadPartRequest().withBucketName("getput").withPartSize(10).withKey("foo4").withPartNumber(2).withUploadId(init.getUploadId).withInputStream(new ByteArrayInputStream("worldworld".getBytes())))
      val result = s3.completeMultipartUpload(new CompleteMultipartUploadRequest("getput", "foo4", init.getUploadId, List(p1.getPartETag, p2.getPartETag).asJava))
      result.getKey shouldBe "foo4"
      getContent(s3.getObject("getput", "foo4")) shouldBe "hellohelloworldworld"
    }
    it should "work with large blobs" in {
      val init = s3.initiateMultipartUpload(new InitiateMultipartUploadRequest("getput", "fooLarge"))
      val blobs = for (i <- 0 to 200) yield {
        val blob1 = new Array[Byte](10000)
        Random.nextBytes(blob1)
        val p1 = s3.uploadPart(new UploadPartRequest().withBucketName("getput").withPartSize(blob1.length).withKey("fooLarge").withPartNumber(i).withUploadId(init.getUploadId).withInputStream(new ByteArrayInputStream(blob1)))
        blob1 -> p1.getPartETag
      }
      val result = s3.completeMultipartUpload(new CompleteMultipartUploadRequest("getput", "fooLarge", init.getUploadId, blobs.map(_._2).asJava))
      result.getKey shouldBe "fooLarge"
      DigestUtils.md5Hex(s3.getObject("getput", "fooLarge").getObjectContent) shouldBe DigestUtils.md5Hex(blobs.map(_._1).fold(Array[Byte]())(_ ++ _))
    }


    it should "produce NoSuchBucket if bucket does not exist" in {
      val exc = intercept[AmazonS3Exception] {
        val init = s3.initiateMultipartUpload(new InitiateMultipartUploadRequest("aws-404", "foo4"))
        val p1 = s3.uploadPart(new UploadPartRequest().withBucketName("aws-404").withPartSize(10).withKey("foo4").withPartNumber(1).withUploadId(init.getUploadId).withInputStream(new ByteArrayInputStream("hellohello".getBytes())))
      }
      exc.getStatusCode shouldBe 404
      exc.getErrorCode shouldBe "NoSuchBucket"
    }

    it should "upload multipart with metadata" in {
      s3.createBucket("getput")
      val metadata: ObjectMetadata = new ObjectMetadata()
      metadata.setContentType("application/json")
      metadata.addUserMetadata("metamaic", "maic")
      val init = s3.initiateMultipartUpload(new InitiateMultipartUploadRequest("getput", "foo4", metadata))
      val p1 = s3.uploadPart(new UploadPartRequest().withBucketName("getput").withPartSize(10).withKey("foo4").withPartNumber(1).withUploadId(init.getUploadId).withInputStream(new ByteArrayInputStream("hellohello".getBytes())))
      val p2 = s3.uploadPart(new UploadPartRequest().withBucketName("getput").withPartSize(10).withKey("foo4").withPartNumber(2).withUploadId(init.getUploadId).withInputStream(new ByteArrayInputStream("worldworld".getBytes())))
      val result = s3.completeMultipartUpload(new CompleteMultipartUploadRequest("getput", "foo4", init.getUploadId, List(p1.getPartETag, p2.getPartETag).asJava))
      result.getKey shouldBe "foo4"
      val s3Object = s3.getObject("getput", "foo4")
      getContent(s3Object) shouldBe "hellohelloworldworld"

      val actualMetadata: ObjectMetadata = s3Object.getObjectMetadata
      actualMetadata.getContentType shouldBe "application/json"
      actualMetadata.getUserMetadata.get("metamaic") shouldBe "maic"
    }
  }
}
