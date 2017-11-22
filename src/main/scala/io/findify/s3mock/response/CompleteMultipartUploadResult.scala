package io.findify.s3mock.response

import java.net.URLDecoder

/**
  * Created by shutty on 8/10/16.
  */
case class CompleteMultipartUploadResult(bucket:String, key:String, etag:String) {
  def toXML =
    <CompleteMultipartUploadResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
      <Location>http://s3.amazonaws.com/{bucket}/{key}</Location>
      <Bucket>{bucket}</Bucket>
      <Key>{/* the key is the still URLencoded path */URLDecoder.decode(key, "UTF-8") }</Key>
      <ETag>"{etag}"</ETag>
    </CompleteMultipartUploadResult>
}
