package io.findify.s3mock.provider.tags

import com.amazonaws.services.s3.model.{ObjectMetadata, Tag}

import scala.collection.concurrent.TrieMap
import scala.collection.mutable

class InMemoryTagStore extends TagStore {

  private val bucketTags = new TrieMap[String, mutable.Map[String, List[Tag]]]

  override def delete(bucket: String, key: String): Unit = {
    val currentTags = bucketTags.get(bucket)
    currentTags.flatMap(_.remove(key))
  }

  override def get(bucket: String, key: String): Option[List[Tag]] =
    bucketTags.get(bucket).flatMap(_.get(key))

  override def set(bucket: String, key: String, tags: List[Tag]): Unit = {
    val currentTags = bucketTags.getOrElseUpdate(bucket, new TrieMap[String, List[Tag]]())
    currentTags.put(key, tags)
  }

  override def remove(bucket: String): Unit = bucketTags.remove(bucket)

}
