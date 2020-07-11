package io.findify.s3mock.provider

import akka.http.scaladsl.model.DateTime
import com.amazonaws.services.s3.model.ObjectMetadata
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.error.{NoSuchBucketException, NoSuchKeyException, NoSuchVersionException}
import io.findify.s3mock.provider.metadata.{InMemoryVersionedMetadataStore, MetadataStore}

import scala.collection.concurrent.TrieMap
import scala.collection.mutable

/**
  * Created by furkilic on 7/11/20.
  */

class InMemoryVersionedProvider extends AbstractInMemoryProvider with LazyLogging {

  protected case class VersionedBucketContents(creationTime: DateTime, keysInBucket: mutable.Map[String, List[VersionedKeyContents]]) extends BucketContents {
    override def getCreationTime: DateTime = creationTime

    override def getKeyInBucket(key: String): Option[KeyContents] = keysInBucket.get(key).map(_.last)

    override def putContentsInKeyInBucket(key: String, data: Array[Byte], objectMetadata: ObjectMetadata, lastModificationTime: DateTime = DateTime.now): Unit = {
      keysInBucket.put(key, keysInBucket.getOrElse(key, List()) :+ VersionedKeyContents(DateTime.now, data, objectMetadata.getVersionId))
    }

    override def getKeysInBucket: mutable.Map[String, KeyContents] = keysInBucket map { case (name: String, list: List[VersionedKeyContents]) => (name, list.last) }

    override def removeKeyInBucket(key: String): Option[KeyContents] = keysInBucket.remove(key).map(_.last)
  }

  protected case class VersionedKeyContents(lastModificationTime: DateTime, data: Array[Byte], versionId: String) extends KeyContents {
    override def getLastModificationTime: DateTime = lastModificationTime

    override def getData: Array[Byte] = data
  }


  override def newBucketContents(creationTime: DateTime): BucketContents = VersionedBucketContents(creationTime, new TrieMap)

  override def newMetadataStore: MetadataStore = new InMemoryVersionedMetadataStore

  override def getObject(bucket: String, key: String, params: Map[String, String] = Map.empty): GetObjectData = {
    params.get("versionId") match {
      case Some(versionId) => bucketDataStore.get(bucket) match {
        case Some(bucketContent: VersionedBucketContents) => bucketContent.keysInBucket.get(key) match {
          case Some(contentVersions) => contentVersions.find(_.versionId == versionId) match {
            case Some(keyContent) =>
              logger.debug(s"reading object for s://$bucket/$key/$versionId")
              var meta = metadataStore.asInstanceOf[InMemoryVersionedMetadataStore].get(bucket, key, versionId)
              GetObjectData(keyContent.data, meta)
            case None => throw NoSuchVersionException(bucket, key, versionId)
          }
          case None => throw NoSuchKeyException(bucket, key)
        }
        case None => throw NoSuchBucketException(bucket)
      }
      case None => super.getObject(bucket, key)
    }

  }

}
