package io.findify.s3mock.provider
import java.util.UUID

import better.files.File
import io.findify.s3mock.response.{Bucket, GetBuckets}
import org.joda.time.{DateTime, LocalDateTime}

/**
  * Created by shutty on 8/9/16.
  */
class FileProvider(dir:String) extends Provider {
  override def listBuckets: GetBuckets = {
    val buckets = File(dir).list.map(f => Bucket(f.name, new DateTime(f.lastModifiedTime.toEpochMilli)))
    GetBuckets("root", UUID.randomUUID().toString, buckets.toList)
  }
}
