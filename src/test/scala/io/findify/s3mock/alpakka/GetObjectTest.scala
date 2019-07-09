package io.findify.s3mock.alpakka

import akka.http.scaladsl.model.headers.ByteRange
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import io.findify.s3mock.S3MockTest

import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Created by shutty on 5/19/17.
  */
class GetObjectTest extends S3MockTest {

  override def behaviour(fixture: => Fixture) = {
    val s3 = fixture.client
    implicit val sys = fixture.system
    implicit val mat = fixture.mat


    it should "get objects via alpakka" in {
      s3.createBucket("alpakka1")
      s3.putObject("alpakka1", "test1", "foobar")
      val result = Await.result(fixture.alpakka.download("alpakka1", "test1")._1.runWith(Sink.seq), 5.second)
      val str = result.fold(ByteString(""))(_ ++ _).utf8String
      str shouldBe "foobar"
    }

    it should "get by range" in {
      s3.createBucket("alpakka2")
      s3.putObject("alpakka2", "test2", "foobar")
      val result = Await.result(fixture.alpakka.download("alpakka2", "test2", Some(ByteRange(1, 4)))._1.runWith(Sink.seq), 5.second)
      val str = result.fold(ByteString(""))(_ ++ _).utf8String
      str shouldBe "ooba"
    }
  }
}
