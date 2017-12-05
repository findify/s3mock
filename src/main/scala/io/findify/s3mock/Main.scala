package io.findify.s3mock

import better.files.File
import com.amazonaws.auth.{AWSStaticCredentialsProvider, AnonymousAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import io.findify.s3mock.provider.FileProvider

/**
  * Created by shutty on 8/9/16.
  */
object Main {
  val port = sys.env.get("S3MOCK_PORT").map(Integer.parseInt).getOrElse(8001)

  def main(args: Array[String]): Unit = {
    val server = new S3Mock(port, new FileProvider(
      sys.env.get("REPOSITORY_PATH") match {
        case Some(path) => File(path).pathAsString
        case None => File.newTemporaryDirectory(prefix = "s3mock").pathAsString
      }))
    server.start

    sys.env.get("BUCKET_NAME").foreach(name => createBucket(name))
  }

  def createBucket(bucketName: String): Unit = {
    val client = clientFor("localhost", port)
    client.createBucket(bucketName)
    println("Created bucket: " + bucketName)
    client.shutdown()
  }

  def clientFor(host: String, port: Int): AmazonS3 = {
    val endpoint = new EndpointConfiguration(s"http://$host:$port", "eu-west-1")
    AmazonS3ClientBuilder.standard()
      .withPathStyleAccessEnabled(true)
      .withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))
      .withEndpointConfiguration(endpoint)
      .build()
  }
}
