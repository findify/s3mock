package io.findify.s3mock.provider.tags

import com.amazonaws.services.s3.model.Tag

trait TagStore {
  def delete(bucket: String, key: String): Unit
  def get(bucket: String, key: String): Option[List[Tag]]
  def remove(bucket: String): Unit
  def set(bucket: String, key: String, tags: List[Tag]): Unit
}
