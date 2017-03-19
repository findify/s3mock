package io.findify.s3mock.provider.metadata

import com.amazonaws.services.s3.model.ObjectMetadata

import scala.collection.concurrent.TrieMap
import scala.collection.mutable

class InMemoryMetadataStore extends MetadataStore {

  private val bucketMetadata = new TrieMap[String, mutable.Map[String, ObjectMetadata]]

  override def put(bucket: String, key: String, meta: ObjectMetadata): Unit = {
    val currentBucketMetadata = bucketMetadata.getOrElseUpdate(bucket, new TrieMap[String, ObjectMetadata]())
    currentBucketMetadata.put(key, meta)
  }

  override def get(bucket: String, key: String): Option[ObjectMetadata] = {
    bucketMetadata.get(bucket).flatMap(_.get(key))
  }

  override def delete(bucket: String, key: String): Unit = {
    val currentBucketMetadata = bucketMetadata.get(bucket)
    currentBucketMetadata.flatMap(_.remove(key))
  }

  override def remove(bucket: String): Unit = bucketMetadata.remove(bucket)
}
