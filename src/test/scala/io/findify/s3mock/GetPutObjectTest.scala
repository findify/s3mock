package io.findify.s3mock

import java.nio.charset.Charset
import scala.collection.JavaConversions._
import org.apache.commons.io.IOUtils

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
}
