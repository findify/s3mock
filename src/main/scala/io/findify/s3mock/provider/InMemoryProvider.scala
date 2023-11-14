package io.findify.s3mock.provider

import java.time.Instant
import java.util.{Date, UUID}

import org.apache.pekko.http.scaladsl.model.DateTime
import com.amazonaws.services.s3.model.ObjectMetadata
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.error.{NoSuchBucketException, NoSuchKeyException}
import io.findify.s3mock.provider.metadata.{InMemoryMetadataStore, MetadataStore}
import io.findify.s3mock.request.{CompleteMultipartUpload, CreateBucketConfiguration}
import io.findify.s3mock.response._
import org.apache.commons.codec.digest.DigestUtils

import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.util.Random

class InMemoryProvider extends Provider with LazyLogging {
  private val mdStore = new InMemoryMetadataStore
  private val bucketDataStore = new TrieMap[String, BucketContents]
  private val multipartTempStore = new TrieMap[String, mutable.SortedSet[MultipartChunk]]

  private case class BucketContents(creationTime: DateTime, keysInBucket: mutable.Map[String, KeyContents])

  private case class KeyContents(lastModificationTime: DateTime, data: Array[Byte])

  private case class MultipartChunk(partNo: Int, data: Array[Byte]) extends Ordered[MultipartChunk] {
    override def compare(that: MultipartChunk): Int = partNo compareTo that.partNo
  }

  override def metadataStore: MetadataStore = mdStore

  override def listBuckets: ListAllMyBuckets = {
    val buckets = bucketDataStore map { case (name, data) => Bucket(name, data.creationTime) }
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
        val matchingKeys = bucketContent.keysInBucket.filterKeys(_.startsWith(prefix2))
        val matchResults = matchingKeys map { case (name, content) =>
          Content(name, content.lastModificationTime, DigestUtils.md5Hex(content.data), content.data.length, "STANDARD")
        }
        logger.debug(s"listing bucket contents: ${matchResults.map(_.key)}")
        val commonPrefixes = normalizeDelimiter(delimiter) match {
          case Some(del) => matchResults.flatMap(f => commonPrefix(f.key, prefix2, del)).toList.sorted.distinct
          case None => Nil
        }
        val filteredFiles: List[Content] = matchResults.filterNot(f => commonPrefixes.exists(p => f.key.startsWith(p))).toList
        val count = maxkeys.getOrElse(Int.MaxValue)
        val result = filteredFiles.sortBy(_.key)
        ListBucket(bucket, prefix, delimiter, commonPrefixes, result.take(count).take(count), isTruncated = result.size>count)
      case None => throw NoSuchBucketException(bucket)
    }
  }

  override def createBucket(name: String, bucketConfig: CreateBucketConfiguration): CreateBucket = {
    bucketDataStore.putIfAbsent(name, BucketContents(DateTime.now, new TrieMap))
    logger.debug(s"creating bucket $name")
    CreateBucket(name)
  }

  override def putObject(bucket: String, key: String, data: Array[Byte], objectMetadata: ObjectMetadata): Unit = {
    bucketDataStore.get(bucket) match {
      case Some(bucketContent) =>
        logger.debug(s"putting object for s3://$bucket/$key, bytes = ${data.length}")
        bucketContent.keysInBucket.put(key, KeyContents(DateTime.now, data))
        objectMetadata.setLastModified(org.joda.time.DateTime.now().toDate)
        metadataStore.put(bucket, key, objectMetadata)
      case None => throw NoSuchBucketException(bucket)
    }
  }

  override def copyObjectMultipart(sourceBucket: String, sourceKey: String, destBucket: String, destKey: String, part: Int, uploadId:String, fromByte: Int, toByte: Int, newMeta: Option[ObjectMetadata] = None): CopyObjectResult = {
    val data = getObject(sourceBucket, sourceKey).bytes.slice(fromByte, toByte + 1)
    putObjectMultipartPart(destBucket, destKey, part, uploadId, data)
    new CopyObjectResult(DateTime.now, DigestUtils.md5Hex(data))
  }

  override def getObject(bucket: String, key: String): GetObjectData = {
    bucketDataStore.get(bucket) match {
      case Some(bucketContent) => bucketContent.keysInBucket.get(key) match {
        case Some(keyContent) =>
          logger.debug(s"reading object for s://$bucket/$key")
          val meta = metadataStore.get(bucket, key)
          GetObjectData(keyContent.data, meta)
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
        bucketContent.keysInBucket.put(key, KeyContents(DateTime.now, completeBytes))
        multipartTempStore.remove(uploadId)
        logger.debug(s"completed multipart upload for s3://$bucket/$key")
        val hash = DigestUtils.md5Hex(completeBytes)
        metadataStore.get(bucket, key).foreach {m =>
          m.setContentMD5(hash)
          m.setLastModified(org.joda.time.DateTime.now().toDate)
        }
        CompleteMultipartUploadResult(bucket, key, hash)
      case None => throw NoSuchBucketException(bucket)
    }
  }

  override def copyObject(sourceBucket: String, sourceKey: String, destBucket: String, destKey: String, newMeta: Option[ObjectMetadata] = None): CopyObjectResult = {
    (bucketDataStore.get(sourceBucket), bucketDataStore.get(destBucket)) match {
      case (Some(srcBucketContent), Some(dstBucketContent)) =>
        srcBucketContent.keysInBucket.get(sourceKey) match {
          case Some(srcKeyContent) =>
            val destFileModTime = DateTime.now
            dstBucketContent.keysInBucket.put(destKey, KeyContents(destFileModTime, srcKeyContent.data.clone))
            logger.debug(s"Copied s3://$sourceBucket/$sourceKey to s3://$destBucket/$destKey")
            val sourceMeta = newMeta.orElse(metadataStore.get(sourceBucket, sourceKey))
            sourceMeta.foreach(meta => metadataStore.put(destBucket, destKey, meta))
            CopyObjectResult(destFileModTime, DigestUtils.md5Hex(srcKeyContent.data))
          case None => throw NoSuchKeyException(sourceBucket, sourceKey)
        }
      case (None, _) => throw NoSuchBucketException(sourceBucket)
      case _ => throw NoSuchBucketException(destBucket)
    }
  }

  override def deleteObject(bucket: String, key: String): Unit = {
    bucketDataStore.get(bucket) match {
      case Some(bucketContent) => bucketContent.keysInBucket.get(key) match {
        case Some(_) =>
          logger.debug(s"deleting object s://$bucket/$key")
          bucketContent.keysInBucket.remove(key)
          metadataStore.delete(bucket, key)
        case None => bucketContent.keysInBucket.keys.find(_.startsWith(key)) match {
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
