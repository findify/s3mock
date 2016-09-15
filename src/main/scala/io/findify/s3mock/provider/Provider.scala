package io.findify.s3mock.provider

import com.amazonaws.services.s3.model.ObjectMetadata
import io.findify.s3mock.request.{CompleteMultipartUpload, CreateBucketConfiguration}
import io.findify.s3mock.response._

/**
  * Created by shutty on 8/9/16.
  */
trait Provider {
  def listBuckets:ListAllMyBuckets
  def listBucket(bucket:String, prefix:String):ListBucket
  def createBucket(name:String, bucketConfig:CreateBucketConfiguration):CreateBucket
  def putObject(bucket:String, key:String, data:Array[Byte], metadata: ObjectMetadata = null):Unit
  def getObject(bucket:String, key:String):Array[Byte]
  def getMetaData(bucket:String, key:String):ObjectMetadata
  def putObjectMultipartStart(bucket:String, key:String):InitiateMultipartUploadResult
  def putObjectMultipartPart(bucket:String, key:String, partNumber:Int, uploadId:String, data:Array[Byte]):Unit
  def putObjectMultipartComplete(bucket:String, key:String, uploadId:String, request:CompleteMultipartUpload):CompleteMultipartUploadResult
  def deleteObject(bucket:String, key:String):Unit
  def deleteBucket(bucket:String):Unit
}
