package io.findify.s3mock.provider
import java.nio.charset.Charset
import java.util.UUID

import better.files.File
import io.findify.s3mock.request.CreateBucketConfiguration
import io.findify.s3mock.response._
import org.apache.commons.io.IOUtils
import org.joda.time.{DateTime, LocalDateTime}

import scala.io.Source

/**
  * Created by shutty on 8/9/16.
  */
class FileProvider(dir:String) extends Provider {
  def listBuckets: ListAllMyBuckets = {
    val buckets = File(dir).list.map(f => Bucket(f.name, new DateTime(f.lastModifiedTime.toEpochMilli)))
    ListAllMyBuckets("root", UUID.randomUUID().toString, buckets.toList)
  }

  def listBucket(bucket: String, prefix: String) = {
    val files = File(s"$dir/$prefix").list.map(f => Content(f.name, new DateTime(f.lastModifiedTime.toEpochMilli), "0", f.size, "STANDARD"))
    ListBucket(bucket, prefix, files.toList)
  }

  def createBucket(name:String, bucketConfig:CreateBucketConfiguration) = {
    File(s"$dir/$name").createDirectory()
    CreateBucket(name)
  }
  def putObject(bucket:String, key:String, data:String): Unit = {
    val file = File(s"$dir/$bucket/$key")
    if (!file.exists) {
      def create(path:String, dirs:List[String]):Unit = dirs match {
        case fname :: Nil => Unit
        case head :: tail =>
          val current = File(s"$path/$head")
          if (!current.exists) current.createDirectory()
          create(s"$path/$head", tail)
      }
      create(s"$dir", key.split("/").toList)
    }
    file.write(data)
  }
  def getObject(bucket:String, key:String):String = {
    val file = File(s"$dir/$bucket/$key")
    IOUtils.toString(file.newInputStream, Charset.forName("UTF-8"))
  }

}
