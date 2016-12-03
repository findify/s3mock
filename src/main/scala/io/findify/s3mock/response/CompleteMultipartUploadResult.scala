package io.findify.s3mock.response

/**
  * Created by shutty on 8/10/16.
  */
case class CompleteMultipartUploadResult(bucket:String, key:String, etag:String) {
  def toXML =
    <CompleteMultipartUploadResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
      <Location>http://s3.amazonaws.com/{bucket}/{key}</Location>
      <Bucket>{bucket}</Bucket>
      <Key>{key}</Key>
      <ETag>"{etag}"</ETag>
    </CompleteMultipartUploadResult>
}
