package io.findify.s3mock.provider.metadata

import com.amazonaws.services.s3.model.ObjectMetadata

/**
  * Created by shutty on 3/13/17.
  */
trait MetadataStore {
  def put(bucket: String, key: String, meta: ObjectMetadata): Unit
  def get(bucket: String, key: String): Option[ObjectMetadata]
}
