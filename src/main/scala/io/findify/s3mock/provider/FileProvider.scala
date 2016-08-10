package io.findify.s3mock.provider
import java.nio.charset.Charset
import java.util.{Base64, UUID}

import better.files.File
import io.findify.s3mock.request.{CompleteMultipartUpload, CreateBucketConfiguration}
import io.findify.s3mock.response._
import org.apache.commons.io.IOUtils
import org.joda.time.{DateTime, LocalDateTime}

import scala.io.Source
import scala.util.Random

/**
  * Created by shutty on 8/9/16.
  */
class FileProvider(dir:String) extends Provider {
  def listBuckets: ListAllMyBuckets = {
    val buckets = File(dir).list.map(f => Bucket(f.name, new DateTime(f.lastModifiedTime.toEpochMilli)))
    ListAllMyBuckets("root", UUID.randomUUID().toString, buckets.toList)
  }

  def listBucket(bucket: String, prefix: String) = {
    val files = File(s"$dir/$prefix").list.filterNot(_.name.startsWith(".")).map(f => Content(f.name, new DateTime(f.lastModifiedTime.toEpochMilli), "0", f.size, "STANDARD"))
    ListBucket(bucket, prefix, files.toList)
  }

  def createBucket(name:String, bucketConfig:CreateBucketConfiguration) = {
    val bucket = File(s"$dir/$name")
    if (!bucket.exists) bucket.createDirectory()
    CreateBucket(name)
  }
  def putObject(bucket:String, key:String, data:String): Unit = {
    createDir(s"$dir/$bucket/$key")
    val file = File(s"$dir/$bucket/$key")
    file.write(data)
  }
  def getObject(bucket:String, key:String):String = {
    val file = File(s"$dir/$bucket/$key")
    IOUtils.toString(file.newInputStream, Charset.forName("UTF-8"))
  }
  def putObjectMultipartStart(bucket:String, key:String):InitiateMultipartUploadResult = {
    val id = Math.abs(Random.nextLong()).toString
    createDir(s"$dir/.mp/$bucket/$key/$id/.keep")
    InitiateMultipartUploadResult(bucket, key, id)
  }
  def putObjectMultipartPart(bucket:String, key:String, partNumber:Int, uploadId:String, data:String) = {
    val file = File(s"$dir/.mp/$bucket/$key/$uploadId/$partNumber")
    file.write(data)
  }
  def putObjectMultipartComplete(bucket:String, key:String, uploadId:String, request:CompleteMultipartUpload) = {
    val files = request.parts.map(part => File(s"$dir/.mp/$bucket/$key/$uploadId/${part.partNumber}"))
    val parts = files.map(f => IOUtils.toString(f.newInputStream, Charset.forName("UTF-8")))
    createDir(s"$dir/$bucket/$key")
    val file = File(s"$dir/$bucket/$key")
    file.write(parts.mkString)
    File(s"$dir/.mp/$bucket/$key").delete()
    CompleteMultipartUploadResult(bucket, key, "")
  }

  private def createDir(path:String) = {
    if (!File(path).exists) {
      def create(path:String, dirs:List[String]):Unit = dirs match {
        case Nil => Unit
        case fname :: Nil => Unit
        case head :: tail =>
          val current = File(s"$path/$head")
          if (!current.exists) current.createDirectory()
          create(s"$path/$head", tail)
      }
      create("", path.split("/").toList)
    }
  }
}
