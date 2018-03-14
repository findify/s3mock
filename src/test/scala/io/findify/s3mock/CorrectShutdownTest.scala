package io.findify.s3mock

object CorrectShutdownTest {
  def main(args: Array[String]): Unit = {
    val s3mock = S3Mock.create(8080)
    s3mock.start
    s3mock.shutdown
  }
}
