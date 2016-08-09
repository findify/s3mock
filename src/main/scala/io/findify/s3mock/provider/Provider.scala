package io.findify.s3mock.provider

import io.findify.s3mock.response.GetBuckets

/**
  * Created by shutty on 8/9/16.
  */
trait Provider {
  def listBuckets:GetBuckets
}
