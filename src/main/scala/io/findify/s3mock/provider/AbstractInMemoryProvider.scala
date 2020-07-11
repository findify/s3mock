package io.findify.s3mock.provider

import java.util.UUID

import akka.http.scaladsl.model.DateTime
import com.amazonaws.services.s3.model.ObjectMetadata
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.error.{NoSuchBucketException, NoSuchKeyException}
import io.findify.s3mock.provider.metadata.MetadataStore
import io.findify.s3mock.request.{CompleteMultipartUpload, CreateBucketConfiguration}
import io.findify.s3mock.response._
import org.apache.commons.codec.digest.DigestUtils

import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.util.Random

abstract class AbstractInMemoryProvider extends Provider with LazyLogging {
  protected val mdStore = newMetadataStore
  protected val bucketDataStore = new TrieMap[String, BucketContents]
  protected val multipartTempStore = new TrieMap[String, mutable.SortedSet[MultipartChunk]]

  protected trait BucketContents {
    def getCreationTime: DateTime

    def getKeyInBucket(key: String): Option[KeyContents]

    def putContentsInKeyInBucket(key: String, data: Array[Byte], objectMetadata: ObjectMetadata, lastModificationTime: DateTime = DateTime.now): Unit

    def getKeysInBucket: mutable.Map[String, KeyContents]

    def removeKeyInBucket(key: String): Option[KeyContents]
  }

  protected trait KeyContents {
    def getLastModificationTime: DateTime

    def getData: Array[Byte]
  }

  protected case class MultipartChunk(partNo: Int, data: Array[Byte]) extends Ordered[MultipartChunk] {
    override def compare(that: MultipartChunk): Int = partNo compareTo that.partNo
  }

  def newBucketContents(creationTime: DateTime): BucketContents

  def newMetadataStore: MetadataStore

  override def metadataStore: MetadataStore = mdStore

  override def listBuckets: ListAllMyBuckets = {
    val buckets = bucketDataStore map { case (name, data: BucketContents) => Bucket(name, data.getCreationTime) }
    logger.debug(s"listing buckets: ${buckets.map(_.name)}")
    ListAllMyBuckets("root", UUID.randomUUID().toString, buckets.toList)
  }

  override def listBucket(bucket: String, prefix: Option[String], delimiter: Option[String], maxkeys: Option[Int]): ListBucket = {
    def commonPrefix(dir: String, p: String, d: String): Option[String] = {
      dir.indexOf(d, p.length) match {
        case -1 => None
        case pos => Some(p + dir.substring(p.length, pos) + d)
      }
    }

    val prefix2 = prefix.getOrElse("")
    bucketDataStore.get(bucket) match {
      case Some(bucketContent) =>
        val matchingKeys = bucketContent.getKeysInBucket.filterKeys(_.startsWith(prefix2))
        val matchResults = matchingKeys map { case (name, contentVersions: KeyContents) =>
          Content(name, contentVersions.getLastModificationTime, DigestUtils.md5Hex(contentVersions.getData), contentVersions.getData.length, "STANDARD")
        }
        logger.debug(s"listing bucket contents: ${matchResults.map(_.key)}")
        val commonPrefixes = normalizeDelimiter(delimiter) match {
          case Some(del) => matchResults.flatMap(f => commonPrefix(f.key, prefix2, del)).toList.sorted.distinct
          case None => Nil
        }
        val filteredFiles: List[Content] = matchResults.filterNot(f => commonPrefixes.exists(p => f.key.startsWith(p))).toList
        val count = maxkeys.getOrElse(Int.MaxValue)
        val result = filteredFiles.sortBy(_.key)
        ListBucket(bucket, prefix, delimiter, commonPrefixes, result.take(count).take(count), isTruncated = result.size > count)
      case None => throw NoSuchBucketException(bucket)
    }
  }

  override def createBucket(name: String, bucketConfig: CreateBucketConfiguration): CreateBucket = {
    bucketDataStore.putIfAbsent(name, newBucketContents(DateTime.now))
    logger.debug(s"creating bucket $name")
    CreateBucket(name)
  }

  override def putObject(bucket: String, key: String, data: Array[Byte], objectMetadata: ObjectMetadata): Unit = {
    bucketDataStore.get(bucket) match {
      case Some(bucketContent) =>
        logger.debug(s"putting object for s3://$bucket/$key, bytes = ${data.length}")
        bucketContent.putContentsInKeyInBucket(key, data, objectMetadata)
        objectMetadata.setLastModified(org.joda.time.DateTime.now().toDate)
        metadataStore.put(bucket, key, objectMetadata)
      case None => throw NoSuchBucketException(bucket)
    }
  }

  override def copyObjectMultipart(sourceBucket: String, sourceKey: String, destBucket: String, destKey: String, part: Int, uploadId: String, fromByte: Int, toByte: Int, newMeta: Option[ObjectMetadata] = None): CopyObjectResult = {
    val data = getObject(sourceBucket, sourceKey).bytes.slice(fromByte, toByte + 1)
    putObjectMultipartPart(destBucket, destKey, part, uploadId, data)
    new CopyObjectResult(DateTime.now, DigestUtils.md5Hex(data))
  }

  override def getObject(bucket: String, key: String, params: Map[String, String] = Map.empty): GetObjectData = {
    bucketDataStore.get(bucket) match {
      case Some(bucketContent) => bucketContent.getKeyInBucket(key) match {
        case Some(keyContent) =>
          logger.debug(s"reading object for s://$bucket/$key")
          val meta = metadataStore.get(bucket, key)
          GetObjectData(keyContent.getData, meta)
        case None => throw NoSuchKeyException(bucket, key)
      }
      case None => throw NoSuchBucketException(bucket)
    }
  }

  override def putObjectMultipartStart(bucket: String, key: String, metadata: ObjectMetadata): InitiateMultipartUploadResult = {
    bucketDataStore.get(bucket) match {
      case Some(_) =>
        val id = Math.abs(Random.nextLong()).toString
        multipartTempStore.putIfAbsent(id, new mutable.TreeSet)
        metadataStore.put(bucket, key, metadata)
        logger.debug(s"starting multipart upload for s3://$bucket/$key")
        InitiateMultipartUploadResult(bucket, key, id)
      case None => throw NoSuchBucketException(bucket)
    }
  }

  override def putObjectMultipartPart(bucket: String, key: String, partNumber: Int, uploadId: String, data: Array[Byte]): Unit = {
    bucketDataStore.get(bucket) match {
      case Some(_) =>
        logger.debug(s"uploading multipart chunk $partNumber for s3://$bucket/$key")
        multipartTempStore.getOrElseUpdate(uploadId, new mutable.TreeSet).add(MultipartChunk(partNumber, data))
      case None => throw NoSuchBucketException(bucket)
    }
  }

  override def putObjectMultipartComplete(bucket: String, key: String, uploadId: String, request: CompleteMultipartUpload): CompleteMultipartUploadResult = {
    bucketDataStore.get(bucket) match {
      case Some(bucketContent) =>
        val completeBytes = multipartTempStore(uploadId).toSeq.map(_.data).fold(Array[Byte]())(_ ++ _)
        val objectMetadata: ObjectMetadata = metadataStore.get(bucket, key).get
        bucketContent.putContentsInKeyInBucket(key, completeBytes, objectMetadata)
        multipartTempStore.remove(uploadId)
        logger.debug(s"completed multipart upload for s3://$bucket/$key")
        val hash = DigestUtils.md5Hex(completeBytes)
        metadataStore.get(bucket, key).foreach { m =>
          m.setContentMD5(hash)
          m.setLastModified(org.joda.time.DateTime.now().toDate)
        }
        CompleteMultipartUploadResult(bucket, key, hash, objectMetadata)
      case None => throw NoSuchBucketException(bucket)
    }
  }

  override def copyObject(sourceBucket: String, sourceKey: String, destBucket: String, destKey: String, newMeta: Option[ObjectMetadata] = None): CopyObjectResult = {
    (bucketDataStore.get(sourceBucket), bucketDataStore.get(destBucket)) match {
      case (Some(srcBucketContent), Some(dstBucketContent)) =>
        srcBucketContent.getKeyInBucket(sourceKey) match {
          case Some(srcKeyContent) =>
            val destFileModTime = DateTime.now
            val sourceMeta = newMeta.orElse(metadataStore.get(sourceBucket, sourceKey))
            dstBucketContent.putContentsInKeyInBucket(destKey, srcKeyContent.getData.clone, sourceMeta.get, destFileModTime)
            logger.debug(s"Copied s3://$sourceBucket/$sourceKey to s3://$destBucket/$destKey")
            sourceMeta.foreach(meta => metadataStore.put(destBucket, destKey, meta))
            CopyObjectResult(destFileModTime, DigestUtils.md5Hex(srcKeyContent.getData))
          case None => throw NoSuchKeyException(sourceBucket, sourceKey)
        }
      case (None, _) => throw NoSuchBucketException(sourceBucket)
      case _ => throw NoSuchBucketException(destBucket)
    }
  }

  override def deleteObject(bucket: String, key: String): Unit = {
    bucketDataStore.get(bucket) match {
      case Some(bucketContent) => bucketContent.getKeyInBucket(key) match {
        case Some(_) =>
          logger.debug(s"deleting object s://$bucket/$key")
          bucketContent.removeKeyInBucket(key)
          metadataStore.delete(bucket, key)
        case None => bucketContent.getKeysInBucket.keys.find(_.startsWith(key)) match {
          case Some(_) =>
            logger.debug(s"recursive delete by prefix is not supported by S3")
            ()
          case None =>
            logger.warn(s"key does not exist")
            throw NoSuchKeyException(bucket, key)
        }
      }
      case None => throw NoSuchBucketException(bucket)
    }
  }

  override def deleteBucket(bucket: String): Unit = {
    bucketDataStore.get(bucket) match {
      case Some(_) =>
        logger.debug(s"deleting bucket s://$bucket")
        bucketDataStore.remove(bucket)
        metadataStore.remove(bucket)
      case None => throw NoSuchBucketException(bucket)
    }
  }
}
