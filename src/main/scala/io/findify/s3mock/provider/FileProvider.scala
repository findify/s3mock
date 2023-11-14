package io.findify.s3mock.provider
import java.util.UUID
import java.io.{FileInputStream, File => JFile}

import org.apache.pekko.http.scaladsl.model.DateTime
import better.files.File
import better.files.File.OpenOptions
import com.amazonaws.services.s3.model.ObjectMetadata
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.error.{NoSuchBucketException, NoSuchKeyException}
import io.findify.s3mock.provider.metadata.{MapMetadataStore, MetadataStore}
import io.findify.s3mock.request.{CompleteMultipartUpload, CreateBucketConfiguration}
import io.findify.s3mock.response._
import org.apache.commons.codec.digest.DigestUtils

import scala.util.Random

/**
  * Created by shutty on 8/9/16.
  */
class FileProvider(dir:String) extends Provider with LazyLogging {
  val workDir = File(dir)
  if (!workDir.exists) workDir.createDirectories()

  private val meta = new MapMetadataStore(dir)

  override def metadataStore: MetadataStore = meta

  override def listBuckets: ListAllMyBuckets = {
    val buckets = File(dir).list.map(f => Bucket(fromOs(f.name), DateTime(f.lastModifiedTime.toEpochMilli))).toList
    logger.debug(s"listing buckets: ${buckets.map(_.name)}")
    ListAllMyBuckets("root", UUID.randomUUID().toString, buckets)
  }

  override def listBucket(bucket: String, prefix: Option[String], delimiter: Option[String], maxkeys: Option[Int]) = {
    def commonPrefix(dir: String, p: String, d: String): Option[String] = {
      dir.indexOf(d, p.length) match {
        case -1 => None
        case pos => Some(p + dir.substring(p.length, pos) + d)
      }
    }
    val prefixNoLeadingSlash = prefix.getOrElse("").dropWhile(_ == '/')
    val bucketFile = File(s"$dir/$bucket/")
    if (!bucketFile.exists) throw NoSuchBucketException(bucket)
    val bucketFileString = fromOs(bucketFile.toString)
    val bucketFiles = bucketFile.listRecursively(File.VisitOptions.follow).filter(f => {
        val fString = fromOs(f.toString).drop(bucketFileString.length).dropWhile(_ == '/')
        fString.startsWith(prefixNoLeadingSlash) && !f.isDirectory
      })
    val files = bucketFiles.map(f => {
      val stream = new FileInputStream(f.toJava)
      try {
        val md5 = DigestUtils.md5Hex(stream)
        Content(fromOs(f.toString).drop(bucketFileString.length+1).dropWhile(_ == '/'), DateTime(f.lastModifiedTime.toEpochMilli), md5, f.size, "STANDARD")
      } finally {
        stream.close()
      }
    }).toList
    logger.debug(s"listing bucket contents: ${files.map(_.key)}")
    val commonPrefixes = normalizeDelimiter(delimiter) match {
      case Some(del) => files.flatMap(f => commonPrefix(f.key, prefixNoLeadingSlash, del)).distinct.sorted
      case None => Nil
    }
    val filteredFiles = files.filterNot(f => commonPrefixes.exists(p => f.key.startsWith(p)))
    val count = maxkeys.getOrElse(Int.MaxValue)
    val result = filteredFiles.sortBy(_.key)
    ListBucket(bucket, prefix, delimiter, commonPrefixes, result.take(count), isTruncated = result.size>count)
  }

  override def createBucket(name:String, bucketConfig:CreateBucketConfiguration) = {
    val bucket = File(s"$dir/$name")
    if (!bucket.exists) bucket.createDirectory()
    logger.debug(s"creating bucket $name")
    CreateBucket(name)
  }
  override def putObject(bucket:String, key:String, data:Array[Byte], objectMetadata: ObjectMetadata): Unit = {
    val bucketFile = File(s"$dir/$bucket")
    val file = File(s"$dir/$bucket/$key")
    if (!bucketFile.exists) throw NoSuchBucketException(bucket)
    file.createIfNotExists(createParents = true)
    logger.debug(s"writing file for s3://$bucket/$key to $dir/$bucket/$key, bytes = ${data.length}")
    file.writeByteArray(data)(OpenOptions.default)
    objectMetadata.setLastModified(org.joda.time.DateTime.now().toDate)
    metadataStore.put(bucket, key, objectMetadata)
  }
  override def getObject(bucket:String, key:String): GetObjectData = {
    val bucketFile = File(s"$dir/$bucket")
    val file = File(s"$dir/$bucket/$key")
    logger.debug(s"reading object for s3://$bucket/$key")
    if (!bucketFile.exists) throw NoSuchBucketException(bucket)
    if (!file.exists) throw NoSuchKeyException(bucket, key)
    if (file.isDirectory) throw NoSuchKeyException(bucket, key)
    val meta = metadataStore.get(bucket, key)
    GetObjectData(file.byteArray, meta)
  }

  override def putObjectMultipartStart(bucket:String, key:String, metadata: ObjectMetadata):InitiateMultipartUploadResult = {
    val id = Math.abs(Random.nextLong()).toString
    val bucketFile = File(s"$dir/$bucket")
    if (!bucketFile.exists) throw NoSuchBucketException(bucket)
    File(s"$dir/.mp/$bucket/$key/$id/.keep").createIfNotExists(createParents = true)
    metadataStore.put(bucket, key, metadata)
    logger.debug(s"starting multipart upload for s3://$bucket/$key")
    InitiateMultipartUploadResult(bucket, key, id)
  }
  override def putObjectMultipartPart(bucket:String, key:String, partNumber:Int, uploadId:String, data:Array[Byte]) = {
    val bucketFile = File(s"$dir/$bucket")
    if (!bucketFile.exists) throw NoSuchBucketException(bucket)
    val file = File(s"$dir/.mp/$bucket/$key/$uploadId/$partNumber")
    logger.debug(s"uploading multipart chunk $partNumber for s3://$bucket/$key")
    file.writeByteArray(data)(OpenOptions.default)
  }

  override def putObjectMultipartComplete(bucket:String, key:String, uploadId:String, request:CompleteMultipartUpload): CompleteMultipartUploadResult = {
    val bucketFile = File(s"$dir/$bucket")
    if (!bucketFile.exists) throw NoSuchBucketException(bucket)
    val files = request.parts.map(part => File(s"$dir/.mp/$bucket/$key/$uploadId/${part.partNumber}"))
    val parts = files.map(f => f.byteArray)
    val file = File(s"$dir/$bucket/$key")
    file.createIfNotExists(createParents = true)
    val data = parts.fold(Array[Byte]())(_ ++ _)
    file.writeBytes(data.toIterator)
    File(s"$dir/.mp/$bucket/$key").delete()
    val hash = file.md5
    metadataStore.get(bucket, key).foreach {m =>
      m.setContentMD5(hash)
      m.setLastModified(org.joda.time.DateTime.now().toDate)
    }
    logger.debug(s"completed multipart upload for s3://$bucket/$key")
    CompleteMultipartUploadResult(bucket, key, hash)
  }

  override def copyObject(sourceBucket: String, sourceKey: String, destBucket: String, destKey: String, newMeta: Option[ObjectMetadata] = None): CopyObjectResult = {
    val sourceBucketFile = File(s"$dir/$sourceBucket")
    val destBucketFile = File(s"$dir/$destBucket")
    if (!sourceBucketFile.exists) throw NoSuchBucketException(sourceBucket)
    if (!destBucketFile.exists) throw NoSuchBucketException(destBucket)
    val sourceFile = File(s"$dir/$sourceBucket/$sourceKey")
    val destFile = File(s"$dir/$destBucket/$destKey")
    destFile.createIfNotExists(createParents = true)
    sourceFile.copyTo(destFile, overwrite = true)
    logger.debug(s"Copied s3://$sourceBucket/$sourceKey to s3://$destBucket/$destKey")
    val sourceMeta = newMeta.orElse(metadataStore.get(sourceBucket, sourceKey))
    sourceMeta.foreach(meta => metadataStore.put(destBucket, destKey, meta))
    CopyObjectResult(DateTime(sourceFile.lastModifiedTime.toEpochMilli), destFile.md5)
  }


  override def copyObjectMultipart(sourceBucket: String, sourceKey: String, destBucket: String, destKey: String, part: Int, uploadId:String, fromByte: Int, toByte: Int, newMeta: Option[ObjectMetadata] = None): CopyObjectResult = {
    val data = getObject(sourceBucket, sourceKey).bytes.slice(fromByte, toByte + 1)
    putObjectMultipartPart(destBucket, destKey, part, uploadId, data)
    new CopyObjectResult(DateTime.now, DigestUtils.md5Hex(data))
  }

  override def deleteObject(bucket:String, key:String): Unit = {
    val file = File(s"$dir/$bucket/$key")
    logger.debug(s"deleting object s://$bucket/$key")
    if (!file.exists) throw NoSuchKeyException(bucket, key)
    if (!file.isDirectory) {
      file.delete()
      metadataStore.delete(bucket, key)
    }
  }

  override def deleteBucket(bucket:String): Unit = {
    val bucketFile = File(s"$dir/$bucket")
    logger.debug(s"deleting bucket s://$bucket")
    if (!bucketFile.exists) throw NoSuchBucketException(bucket)
    bucketFile.delete()
    metadataStore.remove(bucket)
  }

  /** Replace the os separator with a '/' */
  private def fromOs(path: String): String = {
    path.replace(JFile.separatorChar, '/')
  }

}
