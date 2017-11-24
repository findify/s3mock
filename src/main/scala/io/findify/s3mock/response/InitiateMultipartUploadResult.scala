package io.findify.s3mock.response

import java.net.URLDecoder

/**
  * Created by shutty on 8/10/16.
  */
case class InitiateMultipartUploadResult(bucket:String, key:String, uploadId:String) {
  def toXML =
    <InitiateMultipartUploadResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
      <Bucket>{bucket}</Bucket>
      <Key>{/* the key is the still URLencoded path */URLDecoder.decode(key, "UTF-8") }</Key>
      <UploadId>{uploadId}</UploadId>
    </InitiateMultipartUploadResult>
}
