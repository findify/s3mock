package io.findify.s3mock

import com.typesafe.config.ConfigFactory

/*
  a repro for
  https://github.com/findify/s3mock/issues/56
  Not yet fixed :(
 */

class TypesafeConfigTest  extends S3MockTest {
  override def behaviour(fixture: => Fixture) = {

    it should "load typesafe config files" in {
      val config = ConfigFactory.load("test.conf")
      config.getString("foo.testConfig") shouldBe "test"
      val conf = ConfigFactory.parseResources("test.conf")
      conf.getString("foo.testConfig") shouldBe "test"
    }
  }
}
