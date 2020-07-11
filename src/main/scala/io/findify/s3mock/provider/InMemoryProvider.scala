package io.findify.s3mock.provider

import akka.http.scaladsl.model.DateTime
import com.amazonaws.services.s3.model.ObjectMetadata
import com.typesafe.scalalogging.LazyLogging
import io.findify.s3mock.provider.metadata.{InMemoryMetadataStore, MetadataStore}

import scala.collection.concurrent.TrieMap
import scala.collection.mutable

class InMemoryProvider extends AbstractInMemoryProvider with LazyLogging {

  protected case class SimpleBucketContents(creationTime: DateTime, keysInBucket: mutable.Map[String, KeyContents]) extends BucketContents {
    override def getCreationTime: DateTime = creationTime

    override def getKeyInBucket(key: String): Option[KeyContents] = keysInBucket.get(key)

    override def putContentsInKeyInBucket(key: String, data: Array[Byte], objectMetadata: ObjectMetadata, lastModificationTime: DateTime = DateTime.now): Unit = {
      keysInBucket.put(key, SimpleKeyContents(lastModificationTime, data))
    }

    override def getKeysInBucket: mutable.Map[String, KeyContents] = keysInBucket

    override def removeKeyInBucket(key: String): Option[KeyContents] = keysInBucket.remove(key)
  }

  protected case class SimpleKeyContents(lastModificationTime: DateTime, data: Array[Byte]) extends KeyContents {
    override def getLastModificationTime: DateTime = lastModificationTime

    override def getData: Array[Byte] = data
  }


  override def newBucketContents(creationTime: DateTime): BucketContents = SimpleBucketContents(creationTime, new TrieMap)

  override def newMetadataStore: MetadataStore = new InMemoryMetadataStore
}
