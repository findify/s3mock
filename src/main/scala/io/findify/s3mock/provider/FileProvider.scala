package io.findify.s3mock.provider
import java.io.{ObjectInputStream, ObjectOutputStream}
import java.util.UUID

import akka.http.scaladsl.model.DateTime
import better.files.File
import better.files.File.OpenOptions
import com.amazonaws.services.s3.model.ObjectMetadata
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.error.{NoSuchBucketException, NoSuchKeyException}
import io.findify.s3mock.provider.metadata.{MapMetadataStore, MetadataStore}
import io.findify.s3mock.request.{CompleteMultipartUpload, CreateBucketConfiguration}
import io.findify.s3mock.response._

import scala.util.Random

/**
  * Created by shutty on 8/9/16.
  */
class FileProvider(dir:String) extends Provider with LazyLogging {
  val metadataStore: MetadataStore = new MapMetadataStore(dir)
  val workDir = File(dir)
  if (!workDir.exists) workDir.createDirectories()

  def listBuckets: ListAllMyBuckets = {
    val buckets = File(dir).list.map(f => Bucket(f.name, DateTime(f.lastModifiedTime.toEpochMilli))).toList
    logger.debug(s"listing buckets: ${buckets.map(_.name)}")
    ListAllMyBuckets("root", UUID.randomUUID().toString, buckets)
  }

  def listBucket(bucket: String, prefix: String) = {
    val prefixNoLeadingSlash = prefix.dropWhile(_ == '/')
    val bucketFile = File(s"$dir/$bucket/")
    if (!bucketFile.exists) throw NoSuchBucketException(bucket)
    val bucketFileString = bucketFile.toString
    val bucketFiles = bucketFile.listRecursively.filter(f => {
        val fString = f.toString.drop(bucketFileString.length).dropWhile(_ == '/')
        fString.startsWith(prefixNoLeadingSlash) && !fString.startsWith(".") && !f.isDirectory
      })
    val files = bucketFiles.map(f => {Content(f.toString.drop(bucketFileString.length+1).dropWhile(_ == '/'), DateTime(f.lastModifiedTime.toEpochMilli), "0", f.size, "STANDARD")})
    logger.debug(s"listing bucket contents: ${files.map(_.key)}")
    ListBucket(bucket, prefix, files.toList)
  }

  def createBucket(name:String, bucketConfig:CreateBucketConfiguration) = {
    val bucket = File(s"$dir/$name")
    if (!bucket.exists) bucket.createDirectory()
    logger.debug(s"creating bucket $name")
    CreateBucket(name)
  }
  def putObject(bucket:String, key:String, data:Array[Byte], objectMetadata: ObjectMetadata = null): Unit = {
    val bucketFile = File(s"$dir/$bucket")
    val file = File(s"$dir/$bucket/$key")
    if (!bucketFile.exists) throw NoSuchBucketException(bucket)
    file.createIfNotExists(createParents = true)
    logger.debug(s"writing file for s3://$bucket/$key to $dir/$bucket/$key, bytes = ${data.length}")
    file.writeByteArray(data)(OpenOptions.default)

    if(objectMetadata != null) putMetaData(bucket, key, objectMetadata)
  }
  def getObject(bucket:String, key:String):Array[Byte] = {
    val bucketFile = File(s"$dir/$bucket")
    val file = File(s"$dir/$bucket/$key")
    logger.debug(s"reading object for s://$bucket/$key")
    if (!bucketFile.exists) throw NoSuchBucketException(bucket)
    if (!file.exists) throw NoSuchKeyException(bucket, key)
    file.byteArray
  }

  def getMetaData(bucket:String, key:String): Option[ObjectMetadata] = {
    metadataStore.get(bucket, key)
  }

  def putMetaData(bucket:String, key:String, meta: ObjectMetadata) = {
    metadataStore.put(bucket, key, meta)
  }

  def putObjectMultipartStart(bucket:String, key:String):InitiateMultipartUploadResult = {
    val id = Math.abs(Random.nextLong()).toString
    val bucketFile = File(s"$dir/$bucket")
    if (!bucketFile.exists) throw NoSuchBucketException(bucket)
    File(s"$dir/.mp/$bucket/$key/$id/.keep").createIfNotExists(createParents = true)
    logger.debug(s"starting multipart upload for s3://$bucket/$key")
    InitiateMultipartUploadResult(bucket, key, id)
  }
  def putObjectMultipartPart(bucket:String, key:String, partNumber:Int, uploadId:String, data:Array[Byte]) = {
    val bucketFile = File(s"$dir/$bucket")
    if (!bucketFile.exists) throw NoSuchBucketException(bucket)
    val file = File(s"$dir/.mp/$bucket/$key/$uploadId/$partNumber")
    logger.debug(s"uploading multipart chunk $partNumber for s3://$bucket/$key")
    file.writeByteArray(data)(OpenOptions.default)
  }
  def putObjectMultipartComplete(bucket:String, key:String, uploadId:String, request:CompleteMultipartUpload) = {
    val bucketFile = File(s"$dir/$bucket")
    if (!bucketFile.exists) throw NoSuchBucketException(bucket)
    val files = request.parts.map(part => File(s"$dir/.mp/$bucket/$key/$uploadId/${part.partNumber}"))
    val parts = files.map(f => f.byteArray)
    val file = File(s"$dir/$bucket/$key")
    file.createIfNotExists(createParents = true)
    val data = parts.fold(Array[Byte]())(_ ++ _)
    file.writeBytes(data.toIterator)
    File(s"$dir/.mp/$bucket/$key").delete()
    logger.debug(s"completed multipart upload for s3://$bucket/$key")
    CompleteMultipartUploadResult(bucket, key, file.md5)
  }

  def copyObject(sourceBucket: String, sourceKey: String, destBucket: String, destKey: String): CopyObjectResult = {
    val sourceBucketFile = File(s"$dir/$sourceBucket")
    val destBucketFile = File(s"$dir/$destBucket")
    if (!sourceBucketFile.exists) throw NoSuchBucketException(sourceBucket)
    if (!destBucketFile.exists) throw NoSuchBucketException(destBucket)
    val sourceFile = File(s"$dir/$sourceBucket/$sourceKey")
    val destFile = File(s"$dir/$destBucket/$destKey")
    destFile.createIfNotExists(createParents = true)
    sourceFile.copyTo(destFile, overwrite = true)
    logger.debug(s"Copied s3://$sourceBucket/$sourceKey to s3://$destBucket/$destKey")
    val sourceMeta = getMetaData(sourceBucket, sourceKey)
    sourceMeta.foreach(meta => putMetaData(destBucket, destKey, meta))
    CopyObjectResult(DateTime(sourceFile.lastModifiedTime.toEpochMilli), destFile.md5)
  }

  def deleteObject(bucket:String, key:String): Unit = {
    val file = File(s"$dir/$bucket/$key")
    logger.debug(s"deleting object s://$bucket/$key")
    if (!file.exists) throw NoSuchKeyException(bucket, key)
    file.delete()
  }

  def deleteBucket(bucket:String): Unit = {
    val bucketFile = File(s"$dir/$bucket")
    logger.debug(s"deleting bucket s://$bucket")
    if (!bucketFile.exists) throw NoSuchBucketException(bucket)
    bucketFile.delete()
  }

}
