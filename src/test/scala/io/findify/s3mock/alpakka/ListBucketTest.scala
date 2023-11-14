package io.findify.s3mock.alpakka

import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.util.ByteString
import io.findify.s3mock.S3MockTest
import scala.concurrent.duration._
import scala.concurrent.Await

class ListBucketTest extends S3MockTest {
  override def behaviour(fixture: => Fixture) = {
    val s3 = fixture.client
    implicit val sys = fixture.system
    implicit val mat = fixture.mat


    it should "list objects via alpakka" in {
      s3.createBucket("alpakkalist")
      s3.putObject("alpakkalist", "test1", "foobar")
      s3.putObject("alpakkalist", "test2", "foobar")
      s3.putObject("alpakkalist", "test3", "foobar")
      val result = Await.result(fixture.alpakka.listBucket("alpakkalist", None).runWith(Sink.seq), 5.second)
      result.size shouldBe 3
      result.map(_.key) shouldBe Seq("test1", "test2", "test3")
    }

    it should "list objects with prefix" in {
      s3.createBucket("alpakkalist2")
      s3.putObject("alpakkalist2", "test1", "foobar")
      s3.putObject("alpakkalist2", "test2", "foobar")
      s3.putObject("alpakkalist2", "xtest3", "foobar")
      val result = Await.result(fixture.alpakka.listBucket("alpakkalist2", Some("test")).runWith(Sink.seq), 5.second)
      result.size shouldBe 2
      result.map(_.key) shouldBe Seq("test1", "test2")
    }
  }
}
