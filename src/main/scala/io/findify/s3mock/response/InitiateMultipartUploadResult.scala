package io.findify.s3mock.response

/**
  * Created by shutty on 8/10/16.
  */
case class InitiateMultipartUploadResult(bucket:String, key:String, uploadId:String) {
  def toXML =
    <InitiateMultipartUploadResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
      <Bucket>{bucket}</Bucket>
      <Key>{key}</Key>
      <UploadId>{uploadId}</UploadId>
    </InitiateMultipartUploadResult>
}
