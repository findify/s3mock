package io.findify.s3mock

import java.io.ByteArrayInputStream
import java.util

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.stream.ActorMaterializer
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model._
import com.amazonaws.util.IOUtils

import scala.collection.JavaConversions._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source
import scala.util.{Random, Try}

/**
  * Created by shutty on 8/10/16.
  */

class GetPutObjectTest extends S3MockTest {
  override def behaviour(fixture: => Fixture) = {
    val s3 = fixture.client
    val port = fixture.port
    it should "put object" in {
      s3.createBucket("getput").getName shouldBe "getput"
      s3.listBuckets().exists(_.getName == "getput") shouldBe true
      s3.putObject("getput", "foo", "bar")
      val result = getContent(s3.getObject("getput", "foo"))
      result shouldBe "bar"
    }
    it should "be able to post data" in {
      implicit val system = ActorSystem.create("test")
      implicit val mat = ActorMaterializer()
      val http = Http(system)
      if (!s3.listBuckets().exists(_.getName == "getput")) s3.createBucket("getput")
      val response = Await.result(http.singleRequest(HttpRequest(method = HttpMethods.POST, uri = s"http://127.0.0.1:$port/getput/foo2", entity = "bar")), 10.seconds)
      getContent(s3.getObject("getput", "foo2")) shouldBe "bar"
    }
    it should "put objects in subdirs" in {
      s3.putObject("getput", "foo1/foo2/foo3", "bar")
      val result = getContent(s3.getObject("getput", "foo1/foo2/foo3"))
      result shouldBe "bar"
    }
    it should "not drop \\r\\n symbols" in {
      s3.putObject("getput", "foorn", "bar\r\nbaz")
      val result = getContent(s3.getObject("getput", "foorn"))
      result shouldBe "bar\r\nbaz"
    }
    it should "put & get large binary blobs" in {
      val blob = Random.nextString(1024000).getBytes("UTF-8")
      s3.putObject("getput", "foolarge", new ByteArrayInputStream(blob), new ObjectMetadata())
      val result = getContent(s3.getObject("getput", "foolarge")).getBytes("UTF-8")
      result shouldBe blob
    }

    it should "store tags and spit them back on get tagging requests" in {
      s3.createBucket("tbucket")
      s3.putObject(
        new PutObjectRequest("tbucket", "taggedobj", new ByteArrayInputStream("content".getBytes("UTF-8")), new ObjectMetadata)
          .withTagging(new ObjectTagging(List(new Tag("key1", "val1"), new Tag("key=&interesting", "value=something&stragne"))))
      )
      var tagging = s3.getObjectTagging(new GetObjectTaggingRequest("tbucket", "taggedobj")).getTagSet
      var tagMap = new util.HashMap[String, String]()
      for (tag <- tagging) {
        tagMap.put(tag.getKey, tag.getValue)
      }
      tagMap.size() shouldBe 2
      tagMap.get("key1") shouldBe "val1"
      tagMap.get("key=&interesting") shouldBe "value=something&stragne"
    }
    it should "be OK with retrieving tags for un-tagged objects" in {
      s3.putObject("tbucket", "taggedobj", "some-content")
      var tagging = s3.getObjectTagging(new GetObjectTaggingRequest("tbucket", "taggedobj")).getTagSet
      tagging.size() shouldBe 0
    }

    it should "produce NoSuchBucket if bucket does not exist when GETting" in {
      val exc = intercept[AmazonS3Exception] {
        s3.getObject("aws-404", "foo")
      }
      exc.getStatusCode shouldBe 404
      exc.getErrorCode shouldBe "NoSuchBucket"
    }

    it should "produce NoSuchBucket if bucket does not exist when PUTting" in {
      val exc = intercept[AmazonS3Exception] {
        s3.putObject("aws-404", "foo", "content")
      }
      exc.getStatusCode shouldBe 404
      exc.getErrorCode shouldBe "NoSuchBucket"
    }

    it should "work with large files" in {
      val huge = Random.nextString(10 * 1024 * 1024)
      s3.putObject("getput", "foobig", huge)
      val result = getContent(s3.getObject("getput", "foobig"))
      result shouldBe huge
    }

    it should "work with dot-files" in {
      s3.createBucket("dot")
      s3.listBuckets().exists(_.getName == "dot") shouldBe true
      s3.putObject("dot", "foo", "bar")
      s3.putObject("dot", ".foo", "bar")
      val result = s3.listObjects("dot").getObjectSummaries.toList.map(_.getKey)
      result shouldBe List(".foo", "foo")
    }
    it should "support ranged get requests" in {

      val data = new Array[Byte](1000)
      Random.nextBytes(data)

      val bucket = "rangedbuck"
      val key = "data"

      s3.createBucket(bucket)
      s3.putObject(bucket, key, new ByteArrayInputStream(data), new ObjectMetadata())

      val (startByte, endByte) = (5L, 55L)
      val getObjectRequest = new GetObjectRequest(bucket, key)
      getObjectRequest.setRange(startByte, endByte)

      val sliceOfData = data.slice(startByte.toInt, endByte.toInt + 1)
      val retrievedData = IOUtils.toByteArray(s3.getObject(getObjectRequest).getObjectContent)

      retrievedData shouldEqual sliceOfData
    }

    it should "return 404 on subpath request" in {
      s3.createBucket("subpath")
      s3.putObject("subpath", "some/path/example", "bar")
      val noSlash = Try(s3.getObject("subpath", "some/path"))
      noSlash.failed.get.asInstanceOf[AmazonS3Exception].getStatusCode shouldBe 404
      val withSlash = Try(s3.getObject("subpath", "some/path/"))
      withSlash.failed.get.asInstanceOf[AmazonS3Exception].getStatusCode shouldBe 404
    }

    // this trick is not possible on POSIX-compliant file systems:
    // So the test will always fail in file-based provider
    it should "be possible to store /key and /key/bar objects at the same time" ignore {
      s3.createBucket("prefix")
      s3.putObject("prefix", "some/path", "bar")
      s3.putObject("prefix", "some", "bar")
      val noSlash = Try(s3.getObject("prefix", "some/path"))
      val withSlash = Try(s3.getObject("prefix", "some"))
      val br=1
    }

    it should "have etag in metadata" in {
      s3.createBucket("etag")
      s3.putObject("etag", "file/name", "contents")
      val data = s3.getObjectMetadata("etag", "file/name")
      data.getETag shouldBe "98bf7d8c15784f0a3d63204441e1e2aa"
    }

    it should "not fail concurrent requests" in {
      s3.createBucket("concurrent")
      s3.putObject("concurrent", "file/name", "contents")
      val results = Range(1, 100).par.map(_ => IOUtils.toString(s3.getObject("concurrent", "file/name").getObjectContent)).toList
      results.forall(_ == "contents") shouldBe true
    }

    it should "be able to upload files in a single chunk when providing authentication" in {
      val s3c = AmazonS3ClientBuilder
        .standard
        .disableChunkedEncoding
        .withPathStyleAccessEnabled(true)
        .withEndpointConfiguration(new EndpointConfiguration(s"http://127.0.0.1:$port", "us-east-1"))
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("foo", "bar")))
        .build
      s3c.putObject("getput", "foo3", "bar3")
      val result = Source.fromInputStream(s3c.getObject("getput", "foo3").getObjectContent, "UTF-8").mkString
      result shouldBe "bar3"
    }
  }

}

