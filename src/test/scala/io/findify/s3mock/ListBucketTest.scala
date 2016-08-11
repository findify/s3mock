package io.findify.s3mock

import scala.collection.JavaConverters._
import scala.collection.JavaConversions._

/**
  * Created by shutty on 8/9/16.
  */
class ListBucketTest extends S3MockTest {
  "s3 mock" should "list bucket" in {
    s3.createBucket("foo")
    s3.listObjects("foo").getObjectSummaries.isEmpty shouldBe true
  }
  it should "list bucket with prefix" in {
    s3.createBucket("list")
    s3.putObject("list", "foo1", "xxx")
    s3.putObject("list", "foo2", "xxx")
    s3.putObject("list", "xfoo3", "xxx")
    s3.listObjects("list", "foo").getObjectSummaries.asScala.toList.map(_.getKey).forall(_.startsWith("foo")) shouldBe true
  }
  it should "list objects in subfolders with prefix" in {
    s3.createBucket("list2")
    s3.putObject("list2", "one/foo1", "xxx")
    s3.putObject("list2", "one/foo2", "xxx")
    s3.putObject("list2", "one/xfoo3", "xxx")
    s3.listObjects("list2", "one/foo").getObjectSummaries.asScala.toList.map(_.getKey).forall(_.startsWith("foo")) shouldBe true
  }
  it should "return empty list if prefix is incorrect" in {
    s3.createBucket("list3")
    s3.putObject("list3", "one/foo1", "xxx")
    s3.putObject("list3", "one/foo2", "xxx")
    s3.putObject("list3", "one/xfoo3", "xxx")
    s3.listObjects("list3", "qaz/qax").getObjectSummaries.asScala.isEmpty shouldBe true

  }
}
