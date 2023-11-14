package io.findify.s3mock.alpakka

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.model.headers.ByteRange
import org.apache.pekko.stream.ActorMaterializer
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.util.ByteString
import com.typesafe.config.ConfigFactory
import io.findify.s3mock.S3MockTest

import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
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
      val (result,_) =  Await.result(fixture.alpakka.download("alpakka1", "test1").runWith(Sink.head),5.seconds).get
      val str = Await.result(result.runFold(ByteString(""))(_ ++ _),5.seconds).utf8String
      str shouldBe "foobar"
    }

    it should "get by range" in {
      s3.createBucket("alpakka2")
      s3.putObject("alpakka2", "test2", "foobar")
      val (result,_) =  Await.result(fixture.alpakka.download("alpakka2", "test2", Some(ByteRange(1, 4))).runWith(Sink.head),5.seconds).get
      val str = Await.result(result.runFold(ByteString(""))(_ ++ _),5.seconds).utf8String
      str shouldBe "ooba"
    }
  }
}
