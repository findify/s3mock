package io.findify.s3mock

import java.io.ByteArrayInputStream
import java.nio.charset.Charset

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.stream.ActorMaterializer
import com.amazonaws.services.s3.model.{AmazonS3Exception, ObjectMetadata}

import scala.concurrent.duration._
import scala.collection.JavaConversions._
import org.apache.commons.io.IOUtils

import scala.concurrent.Await
import scala.util.Random

/**
  * Created by shutty on 8/10/16.
  */
class GetPutObjectTest extends S3MockTest {
  "s3 mock" should "put object" in {
    s3.createBucket("getput").getName shouldBe "getput"
    s3.listBuckets().exists(_.getName == "getput") shouldBe true
    s3.putObject("getput", "foo", "bar")
    val result = IOUtils.toString(s3.getObject("getput", "foo").getObjectContent, Charset.forName("UTF-8"))
    result shouldBe "bar"
  }
  it should "be able to post data" in {
    implicit val system = ActorSystem.create("test")
    implicit val mat = ActorMaterializer()
    val http = Http(system)
    if (!s3.listBuckets().exists(_.getName == "getput")) s3.createBucket("getput")
    val response = Await.result(http.singleRequest(HttpRequest(method = HttpMethods.POST, uri = "http://127.0.0.1:8001/getput/foo2", entity = "bar")), 10.seconds)
    IOUtils.toString(s3.getObject("getput", "foo2").getObjectContent, Charset.forName("UTF-8")) shouldBe "bar"
  }
  it should "put objects in subdirs" in {
    s3.putObject("getput", "foo1/foo2/foo3", "bar")
    val result = IOUtils.toString(s3.getObject("getput", "foo1/foo2/foo3").getObjectContent, Charset.forName("UTF-8"))
    result shouldBe "bar"
  }
  it should "not drop \\r\\n symbols" in {
    s3.putObject("getput", "foorn", "bar\r\nbaz")
    val result = IOUtils.toString(s3.getObject("getput", "foorn").getObjectContent, Charset.forName("UTF-8"))
    result shouldBe "bar\r\nbaz"
  }
  it should "put & get large binary blobs" in {
    val blob = Random.nextString(1024000).getBytes("UTF-8")
    s3.putObject("getput", "foolarge", new ByteArrayInputStream(blob), new ObjectMetadata())
    val result = IOUtils.toByteArray(s3.getObject("getput", "foolarge").getObjectContent)
    result shouldBe blob
  }

  "get" should "produce NoSuchBucket if bucket does not exist" in {
    val exc = intercept[AmazonS3Exception] {
      s3.getObject("aws-404", "foo")
    }
    exc.getStatusCode shouldBe 404
    exc.getErrorCode shouldBe "NoSuchBucket"
  }

  "put" should "produce NoSuchBucket if bucket does not exist" in {
    val exc = intercept[AmazonS3Exception] {
      s3.putObject("aws-404", "foo", "content")
    }
    exc.getStatusCode shouldBe 404
    exc.getErrorCode shouldBe "NoSuchBucket"
  }

}
