package io.findify.s3mock.provider

import io.findify.s3mock.request.{CompleteMultipartUpload, CreateBucketConfiguration}
import io.findify.s3mock.response._

/**
  * Created by shutty on 8/9/16.
  */
trait Provider {
  def listBuckets:ListAllMyBuckets
  def listBucket(bucket:String, prefix:String):ListBucket
  def createBucket(name:String, bucketConfig:CreateBucketConfiguration):CreateBucket
  def putObject(bucket:String, key:String, data:String):Unit
  def getObject(bucket:String, key:String):String
  def putObjectMultipartStart(bucket:String, key:String):InitiateMultipartUploadResult
  def putObjectMultipartPart(bucket:String, key:String, partNumber:Int, uploadId:String, data:String):Unit
  def putObjectMultipartComplete(bucket:String, key:String, uploadId:String, request:CompleteMultipartUpload):CompleteMultipartUploadResult
}
