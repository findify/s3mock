package io.findify.s3mock.provider

import io.findify.s3mock.request.CreateBucketConfiguration
import io.findify.s3mock.response.{CreateBucket, ListAllMyBuckets, ListBucket}

/**
  * Created by shutty on 8/9/16.
  */
trait Provider {
  def listBuckets:ListAllMyBuckets
  def listBucket(bucket:String, prefix:String):ListBucket
  def createBucket(name:String, bucketConfig:CreateBucketConfiguration):CreateBucket
  def putObject(bucket:String, key:String, data:String):Unit
  def getObject(bucket:String, key:String):String
}
