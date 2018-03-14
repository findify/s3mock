package io.findify.s3mock

import better.files.File
import io.findify.s3mock.provider.FileProvider

/**
  * Created by shutty on 8/9/16.
  */
object Main {
  def main(args: Array[String]): Unit = {
    val server = new S3Mock(8001, new FileProvider(sys.env.getOrElse("S3MOCK_DATA_DIR", File.newTemporaryDirectory(prefix = "s3mock").pathAsString)))
    server.start
  }
}
