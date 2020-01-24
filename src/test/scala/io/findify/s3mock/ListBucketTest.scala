package io.findify.s3mock

import java.util

import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.{AmazonS3Exception, ListObjectsRequest, ListObjectsV2Request, S3ObjectSummary}
import org.joda.time.DateTime

import scala.jdk.CollectionConverters._

/**
  * Created by shutty on 8/9/16.
  */
class ListBucketTest extends S3MockTest {
  override def behaviour(fixture: => Fixture) = {
    val s3 = fixture.client
    it should "list bucket" in {
      s3.createBucket("foo")
      s3.listObjects("foo").getObjectSummaries.isEmpty shouldBe true
    }
    it should "list bucket with prefix" in {
      s3.createBucket("list")
      s3.putObject("list", "foo1", "xxx")
      s3.putObject("list", "foo2", "xxx")
      s3.putObject("list", "xfoo3", "xxx")
      val list = s3.listObjects("list", "foo").getObjectSummaries.asScala.toList
      list.map(_.getKey).forall(_.startsWith("foo")) shouldBe true
    }
    it should "list objects in subfolders with prefix" in {
      s3.createBucket("list2")
      s3.putObject("list2", "one/foo1/1", "xxx")
      s3.putObject("list2", "one/foo2/2", "xxx")
      s3.putObject("list2", "one/foo2/3", "xxx")
      s3.putObject("list2", "one/foo2/4", "xxx")
      s3.putObject("list2", "one/xfoo3", "xxx")
      val ol = s3.listObjects("list2", "one/f").getObjectSummaries.asScala.toList
      ol.size shouldBe 4
      ol.map(_.getKey).forall(_.startsWith("one/foo")) shouldBe true
    }
    it should "return empty list if prefix is incorrect" in {
      s3.createBucket("list3")
      s3.putObject("list3", "one/foo1", "xxx")
      s3.putObject("list3", "one/foo2", "xxx")
      s3.putObject("list3", "one/xfoo3", "xxx")
      s3.listObjects("list3", "qaz/qax").getObjectSummaries.asScala.isEmpty shouldBe true

    }
    it should "return keys with valid keys (when no prefix given)" in {
      s3.createBucket("list4")
      s3.putObject("list4", "one", "xxx")
      val summaries: util.List[S3ObjectSummary] = s3.listObjects("list4").getObjectSummaries
      summaries.size() shouldBe 1
      val summary = summaries.get(0)
      summary.getBucketName shouldBe "list4"
      summary.getKey shouldBe "one"
      summary.getSize shouldBe 3
      summary.getStorageClass shouldBe "STANDARD"

      val returnedKey = summaries.asScala.last.getKey
      s3.getObject("list4", returnedKey).getKey shouldBe "one"
    }

    it should "produce NoSuchBucket if bucket does not exist" in {
      val exc = intercept[AmazonS3Exception] {
        s3.listObjects("aws-404", "qaz/qax")
      }
      exc.getStatusCode shouldBe 404
      exc.getErrorCode shouldBe "NoSuchBucket"
    }

    it should "obey delimiters && prefixes v1" in {
      s3.createBucket("list5")
      s3.putObject("list5", "sample.jpg", "xxx")
      s3.putObject("list5", "photos/2006/January/sample.jpg", "yyy")
      s3.putObject("list5", "photos/2006/February/sample2.jpg", "zzz")
      s3.putObject("list5", "photos/2006/February/sample3.jpg", "zzz")
      s3.putObject("list5", "photos/2006/February/sample4.jpg", "zzz")
      val req1 = new ListObjectsRequest()
      req1.setBucketName("list5")
      req1.setDelimiter("/")
      val list1 = s3.listObjects(req1)
      val summaries1 = list1.getObjectSummaries.asScala.map(_.getKey).toList
      list1.getCommonPrefixes.asScala.toList shouldBe List("photos/")
      summaries1 shouldBe List("sample.jpg")
    }
    it should "obey delimiters && prefixes v2" in {
      s3.createBucket("list5")
      s3.putObject("list5", "sample.jpg", "xxx")
      s3.putObject("list5", "photos/2006/January/sample.jpg", "yyy")
      s3.putObject("list5", "photos/2006/February/sample2.jpg", "zzz")
      s3.putObject("list5", "photos/2006/February/sample3.jpg", "zzz")
      s3.putObject("list5", "photos/2006/February/sample4.jpg", "zzz")
      val req2 = new ListObjectsRequest()
      req2.setBucketName("list5")
      req2.setDelimiter("/")
      req2.setPrefix("photos/2006/")
      val list2 = s3.listObjects(req2)
      val summaries2 = list2.getObjectSummaries.asScala.map(_.getKey).toList
      list2.getCommonPrefixes.asScala.toList shouldBe List("photos/2006/February/", "photos/2006/January/")
      summaries2 shouldBe Nil
    }

    it should "obey delimiters && prefixes v2 (matching real s3)" ignore {
      val s3 = AmazonS3ClientBuilder.defaultClient()
      s3.createBucket("findify-merlin")
      s3.putObject("findify-merlin", "sample.jpg", "xxx")
      s3.putObject("findify-merlin", "photos/2006/January/sample.jpg", "yyy")
      s3.putObject("findify-merlin", "photos/2006/February/sample2.jpg", "zzz")
      s3.putObject("findify-merlin", "photos/2006/February/sample3.jpg", "zzz")
      s3.putObject("findify-merlin", "photos/2006/February/sample4.jpg", "zzz")
      val req2 = new ListObjectsRequest()
      req2.setBucketName("findify-merlin")
      req2.setDelimiter("/")
      req2.setPrefix("photos/")
      val list2 = s3.listObjects(req2)
      val summaries2 = list2.getObjectSummaries.asScala.map(_.getKey).toList
      list2.getCommonPrefixes.asScala.toList shouldBe List("photos/2006/")
      summaries2 shouldBe Nil
    }


    it should "obey delimiters && prefixes v3" in {
      s3.createBucket("list5")
      s3.putObject("list5", "dev/someEvent/2017/03/13/00/_SUCCESS", "xxx")
      s3.putObject("list5", "dev/someEvent/2017/03/13/01/_SUCCESS", "yyy")
      s3.putObject("list5", "dev/someEvent/2016/12/31/23/_SUCCESS", "zzz")
      val req2 = new ListObjectsRequest()
      req2.setBucketName("list5")
      req2.setDelimiter("/")
      req2.setPrefix("dev/")
      val list2 = s3.listObjects(req2)
      val summaries2 = list2.getObjectSummaries.asScala.map(_.getKey).toList
      list2.getCommonPrefixes.asScala.toList shouldBe List("dev/someEvent/")
      summaries2 shouldBe Nil
    }

    it should "list objects in lexicographical order" in {
      s3.createBucket("list6")
      s3.putObject("list6", "b", "xx")
      s3.putObject("list6", "a", "xx")
      s3.putObject("list6", "0", "xx")
      val list = s3.listObjects("list6")
      list.getObjectSummaries.asScala.map(_.getKey).toList shouldBe List("0", "a", "b")
    }

    it should "getCommonPrefixes should return return objects sorted lexicographically" in {
      s3.createBucket("list7")
      s3.putObject("list7", "dev/10/2017/03/13/00/_SUCCESS", "xxx")
      s3.putObject("list7", "dev/10/2017/03/13/01/_SUCCESS", "xxx")
      s3.putObject("list7", "dev/20/2017/03/13/00/_SUCCESS", "xxx")
      s3.putObject("list7", "dev/20/2017/03/13/01/_SUCCESS", "xxx")
      s3.putObject("list7", "dev/30/2017/03/13/00/_SUCCESS", "xxx")
      s3.putObject("list7", "dev/30/2017/03/13/01/_SUCCESS", "xxx")
      s3.putObject("list7", "dev/40/2017/03/13/00/_SUCCESS", "xxx")
      s3.putObject("list7", "dev/40/2017/03/13/01/_SUCCESS", "xxx")
      s3.putObject("list7", "dev/50/2017/03/13/00/_SUCCESS", "xxx")
      s3.putObject("list7", "dev/50/2017/03/13/01/_SUCCESS", "xxx")
      val req2 = new ListObjectsRequest()
      req2.setBucketName("list7")
      req2.setDelimiter("/")
      req2.setPrefix("dev/")
      val list2 = s3.listObjects(req2)
      val summaries2 = list2.getObjectSummaries.asScala.map(_.getKey).toList
      list2.getCommonPrefixes.asScala.toList shouldBe List("dev/10/", "dev/20/", "dev/30/", "dev/40/", "dev/50/")
      summaries2 shouldBe Nil
    }

    it should "obey delimiters && prefixes when prefix equals to files name" in {
      s3.createBucket("list8")
      s3.putObject("list8", "dev/someEvent/2017/03/13/00/_SUCCESS", "xxx")
      val req2 = new ListObjectsRequest()
      req2.setBucketName("list8")
      req2.setDelimiter("/")
      req2.setPrefix("dev/someEvent/2017/03/13/00/_SUCCESS")
      val list2  = s3.listObjects(req2)
      list2.getObjectSummaries.size shouldEqual 1
      list2.getObjectSummaries.asScala.head.getKey shouldEqual "dev/someEvent/2017/03/13/00/_SUCCESS"
    }

    it should "obey withMaxKeys" in {
      s3.createBucket("list7k")
      s3.putObject("list7k", "b", "xx")
      s3.putObject("list7k", "a", "xx")
      s3.putObject("list7k", "c", "xx")
      val request = new ListObjectsV2Request().withBucketName("list7k").withMaxKeys(2)
      val list = s3.listObjectsV2(request)
      list.getObjectSummaries.asScala.map(_.getKey).toList shouldBe List("a", "b")
      list.isTruncated shouldBe true
    }

    it should "have correct etags" in {
      s3.createBucket("list9")
      s3.putObject("list9", "foo1", "xxx")
      s3.putObject("list9", "foo2", "yyy")
      val list = s3.listObjects("list9", "foo").getObjectSummaries.asScala.toList
      list.find(_.getKey == "foo1").map(_.getETag) shouldBe Some("f561aaf6ef0bf14d4208bb46a4ccb3ad")
    }

    it should "set correct last-modified header" in {
      s3.createBucket("list10")
      s3.putObject("list10", "foo", "xxx")
      val list = s3.listObjects("list10").getObjectSummaries.asScala.toList
      list.find(_.getKey == "foo").map(_.getLastModified.after(DateTime.now().minusMinutes(1).toDate)) shouldBe Some(true)
    }
  }
}
