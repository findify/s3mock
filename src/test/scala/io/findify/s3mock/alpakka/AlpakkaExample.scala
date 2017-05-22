package io.findify.s3mock.alpakka

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.Sink
import com.typesafe.config.ConfigFactory
import scala.collection.JavaConverters._

class AlpakkaExample {
  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.parseMap(Map(
      "akka.stream.alpakka.s3.proxy.host" -> "localhost",
      "akka.stream.alpakka.s3.proxy.port" -> 8001,
      "akka.stream.alpakka.s3.proxy.secure" -> false,
      "akka.stream.alpakka.s3.path-style-access" -> true
    ).asJava)
    implicit val system = ActorSystem.create("test", config)
    implicit val mat = ActorMaterializer()
    val s3a = S3Client()
    val contents = s3a.download("bucket", "key").runWith(Sink.reduce(_ ++ _)).map(_.utf8String)
  }
}
