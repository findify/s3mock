package io.findify.s3mock.alpakka

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.ObjectMetadata
import akka.stream.alpakka.s3.scaladsl.{S3}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters._
import scala.concurrent.Future

object AlpakkaExample {
  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.parseMap(Map(
      "akka.stream.alpakka.s3.proxy.host" -> "localhost",
      "akka.stream.alpakka.s3.proxy.port" -> 8001,
      "akka.stream.alpakka.s3.proxy.secure" -> false,
      "akka.stream.alpakka.s3.path-style-access" -> true,
      "akka.stream.alpakka.s3.aws.credentials.provider" -> "static",
      "akka.stream.alpakka.s3.aws.credentials.access-key-id" -> "foo",
      "akka.stream.alpakka.s3.aws.credentials.secret-access-key" -> "bar",
      "akka.stream.alpakka.s3.aws.region.provider" -> "static",
      "akka.stream.alpakka.s3.aws.region.default-region" -> "us-east-1"
    ).asJava)
    implicit val system = ActorSystem.create("test", config)
    implicit val mat = ActorMaterializer()
    import system.dispatcher

    val ddd: Source[Option[(Source[ByteString, NotUsed], ObjectMetadata)], NotUsed] = S3.download("bucket", "key").withAttributes()
    //val s3a = S3Client()
    val asdf: Future[Option[(Source[ByteString, NotUsed], ObjectMetadata)]] = ddd.runWith(Sink.head)
    val bbb: Future[Option[Future[ByteString]]] = asdf.map((l: Option[(Source[ByteString, NotUsed], ObjectMetadata)]) => l.map(b => b._1.runWith(Sink.head)))
  }
}
