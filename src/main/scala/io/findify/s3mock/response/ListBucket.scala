package io.findify.s3mock.response

import org.joda.time.DateTime

/**
  * Created by shutty on 8/9/16.
  */
case class Content(key:String, lastModified:DateTime, md5:String, size:Long, storageClass:String)
case class ListBucket(bucket:String, prefix:String, contents:List[Content]) {
  def toXML =
    <ListBucketResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
      <Name>{bucket}</Name>
      <Prefix>{prefix}</Prefix>
      <KeyCount>{contents.length}</KeyCount>
      <MaxKeys>1000</MaxKeys>
      <IsTruncated>false</IsTruncated>
        {contents.map(content =>
        <Contents>
          <Key>{content.key}</Key>
          <LastModified>{content.lastModified.toString}</LastModified>
          <ETag>{content.md5}</ETag>
          <Size>{content.size}</Size>
          <StorageClass>{content.storageClass}</StorageClass>
        </Contents>
        )}
    </ListBucketResult>
}
