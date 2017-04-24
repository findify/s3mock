package io.findify.s3mock

import scala.collection.JavaConverters._

/**
  * Created by shutty on 8/30/16.
  */
class ListBucketEmptyWorkdirTest extends S3MockTest {
  override def behaviour(fixture: => Fixture) = {
    val s3 = fixture.client
    it should "list bucket with empty prefix" in {
      s3.createBucket("list")
      s3.putObject("list", "foo1", "xxx")
      s3.putObject("list", "foo2", "xxx")
      val list = s3.listObjects("list").getObjectSummaries.asScala.toList
      list.map(_.getKey).forall(_.startsWith("foo")) shouldBe true
    }
  }
}
