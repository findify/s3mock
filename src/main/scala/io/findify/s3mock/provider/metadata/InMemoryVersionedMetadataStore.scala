package io.findify.s3mock.provider.metadata

import com.amazonaws.services.s3.model.ObjectMetadata

import scala.collection.concurrent.TrieMap
import scala.collection.mutable

/**
  * Created by furkilic on 7/11/20.
  */

class InMemoryVersionedMetadataStore extends MetadataStore {

  private val bucketMetadata = new TrieMap[String, mutable.Map[String, List[ObjectMetadata]]]

  override def put(bucket: String, key: String, meta: ObjectMetadata): Unit = {
    val currentBucketMetadata = bucketMetadata.getOrElseUpdate(bucket, new TrieMap[String, List[ObjectMetadata]]())
    currentBucketMetadata.put(key, currentBucketMetadata.getOrElseUpdate(key, List()) :+ meta)
  }

  override def get(bucket: String, key: String): Option[ObjectMetadata] = {
    bucketMetadata.get(bucket).flatMap(_.get(key)).map(_.last)
  }

  override def delete(bucket: String, key: String): Unit = {
    val currentBucketMetadata = bucketMetadata.get(bucket)
    currentBucketMetadata.flatMap(_.remove(key))
  }

  override def remove(bucket: String): Unit = bucketMetadata.remove(bucket)

  def get(bucket: String, key: String, versionId: String): Option[ObjectMetadata] = {
    bucketMetadata.get(bucket).flatMap(_.get(key)).flatMap(_.find(_.getVersionId == versionId))
  }
}
