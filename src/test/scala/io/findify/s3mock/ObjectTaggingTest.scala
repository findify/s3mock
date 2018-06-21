package io.findify.s3mock

import java.io.ByteArrayInputStream
import java.util

import com.amazonaws.services.s3.model._

class ObjectTaggingTest extends S3MockTest {
  override def behaviour(fixture: => Fixture): Unit = {
    val s3 = fixture.client
    it should "handle PUT / GET / DELETE tagging requests" in {

      s3.createBucket("tbucket")
      s3.putObject(
        new PutObjectRequest("tbucket", "taggedobj", new ByteArrayInputStream("content".getBytes("UTF-8")), new ObjectMetadata)
      )

      import scala.collection.JavaConverters._
      s3.setObjectTagging(
        new SetObjectTaggingRequest(
          "tbucket",
          "taggedobj",
          new ObjectTagging(List(new Tag("key1", "val1"), new Tag("key=&interesting", "value=something&stragne")).asJava)))

      var tagging1 = s3.getObjectTagging(new GetObjectTaggingRequest("tbucket", "taggedobj")).getTagSet.asScala
      var tagMap1 = new util.HashMap[String, String]()
      for (tag <- tagging1) {
        tagMap1.put(tag.getKey, tag.getValue)
      }
      tagMap1.size() shouldBe 2
      tagMap1.get("key1") shouldBe "val1"
      tagMap1.get("key=&interesting") shouldBe "value=something&stragne"

      s3.deleteObjectTagging(new DeleteObjectTaggingRequest("tbucket", "taggedobj"))

      var tagging2 = s3.getObjectTagging(new GetObjectTaggingRequest("tbucket", "taggedobj")).getTagSet.asScala
      var tagMap2 = new util.HashMap[String, String]()
      for (tag <- tagging2) {
        tagMap2.put(tag.getKey, tag.getValue)
      }
      tagMap2.size() shouldBe 0

    }
  }
}
