package io.findify.s3mock

import java.io.ByteArrayInputStream
import java.nio.charset.Charset

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import com.amazonaws.services.s3.model.{CompleteMultipartUploadRequest, InitiateMultipartUploadRequest, UploadPartRequest}
import org.apache.commons.io.IOUtils
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Created by shutty on 8/10/16.
  */
class MultipartUploadTest extends S3MockTest {
  implicit val system = ActorSystem.create("test")
  implicit val mat = ActorMaterializer()
  val http = Http(system)

  "s3" should "upload multipart files" in {
    val response1 = Await.result(http.singleRequest(HttpRequest(method = HttpMethods.POST, uri = "http://127.0.0.1:8001/getput/foo2?uploads")), 10.minutes)
    val data = Await.result(response1.entity.dataBytes.fold(ByteString(""))(_ ++ _).runWith(Sink.head), 10.seconds)
    val uploadId = (scala.xml.XML.loadString(data.utf8String) \ "UploadId").text
    val response2 = Await.result(http.singleRequest(HttpRequest(method = HttpMethods.POST, uri = s"http://127.0.0.1:8001/getput/foo2?partNumber=1&uploadId=$uploadId", entity = "foo")), 10.minutes)
    response2.status.intValue() shouldBe 200
    val response3 = Await.result(http.singleRequest(HttpRequest(method = HttpMethods.POST, uri = s"http://127.0.0.1:8001/getput/foo2?partNumber=2&uploadId=$uploadId", entity = "boo")), 10.minutes)
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
    val response4 = Await.result(http.singleRequest(HttpRequest(method = HttpMethods.POST, uri = s"http://127.0.0.1:8001/getput/foo2?uploadId=$uploadId", entity = commit)), 10.minutes)
    response4.status.intValue() shouldBe 200

    IOUtils.toString(s3.getObject("getput", "foo2").getObjectContent, Charset.forName("UTF-8")) shouldBe "fooboo"
  }

  it should "work with java sdk" in {
    s3.createBucket("getput")
    val init = s3.initiateMultipartUpload(new InitiateMultipartUploadRequest("getput", "foo4"))
    val p1 = s3.uploadPart(new UploadPartRequest().withBucketName("getput").withPartSize(5).withKey("foo4").withPartNumber(1).withUploadId(init.getUploadId).withInputStream(new ByteArrayInputStream("hellohello".getBytes())))
    val p2 = s3.uploadPart(new UploadPartRequest().withBucketName("getput").withPartSize(5).withKey("foo4").withPartNumber(2).withUploadId(init.getUploadId).withInputStream(new ByteArrayInputStream("worldworld".getBytes())))
    val result = s3.completeMultipartUpload(new CompleteMultipartUploadRequest("getput", "foo4", init.getUploadId, List(p1.getPartETag, p2.getPartETag).asJava))
    result.getKey shouldBe "foo4"
    IOUtils.toString(s3.getObject("getput", "foo4").getObjectContent, Charset.forName("UTF-8")) shouldBe "helloworld"
  }
}
