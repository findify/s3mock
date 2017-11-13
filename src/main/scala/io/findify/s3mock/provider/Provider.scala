package io.findify.s3mock.provider

import com.amazonaws.services.s3.model.ObjectMetadata
import io.findify.s3mock.provider.metadata.MetadataStore
import io.findify.s3mock.request.{CompleteMultipartUpload, CreateBucketConfiguration}
import io.findify.s3mock.response._


case class GetObjectData(bytes: Array[Byte], metadata: Option[ObjectMetadata])

/**
  * Interface for provider implementations.
  */
trait Provider {
  def metadataStore: MetadataStore
  def listBuckets:ListAllMyBuckets
  def listBucket(bucket:String, prefix:Option[String], delimiter: Option[String], maxkeys: Option[Int]):ListBucket
  def createBucket(name:String, bucketConfig:CreateBucketConfiguration):CreateBucket
  def putObject(bucket:String, key:String, data:Array[Byte], metadata: ObjectMetadata):Unit
  def getObject(bucket:String, key:String): GetObjectData
  def putObjectMultipartStart(bucket:String, key:String):InitiateMultipartUploadResult
  def putObjectMultipartPart(bucket:String, key:String, partNumber:Int, uploadId:String, data:Array[Byte]):Unit
  def putObjectMultipartComplete(bucket:String, key:String, uploadId:String, request:CompleteMultipartUpload):CompleteMultipartUploadResult
  def deleteObject(bucket:String, key:String):Unit
  def deleteBucket(bucket:String):Unit
  def copyObject(sourceBucket: String, sourceKey: String, destBucket: String, destKey: String, newMeta: Option[ObjectMetadata] = None): CopyObjectResult
  def copyObjectMultipart(sourceBucket: String, sourceKey: String, destBucket: String, destKey: String,  partNumber:Int, uploadId:String, fromByte: Int, toByte:Int,  meta: Option[ObjectMetadata] = None): CopyObjectResult
}


