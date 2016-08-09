package io.findify.s3mock

import io.findify.s3mock.provider.FileProvider

/**
  * Created by shutty on 8/9/16.
  */
object Main {
  def main(args: Array[String]): Unit = {
    val server = new S3Mock(8001, new FileProvider("/tmp/s3mock"))
    server.start
  }
}
