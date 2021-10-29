package io.findify.s3mock.response

import akka.http.scaladsl.model.DateTime


/**
  * Created by shutty on 8/9/16.
  */
case class Content(key:String, lastModified:DateTime, md5:String, size:Long, storageClass:String)
case class ListBucket(bucket:String, prefix: Option[String], delimiter: Option[String], commonPrefixes: List[String], contents:List[Content], isTruncated: Boolean) {
  def toXML =
    <ListBucketResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
      <Name>{bucket}</Name>
      { if (prefix.isDefined) <Prefix>{prefix.get}</Prefix> }
      { if (delimiter.isDefined) <Delimiter>{delimiter.get}</Delimiter> }
      { if (commonPrefixes.nonEmpty)
      {commonPrefixes.map(cp =>
      <CommonPrefixes>
        <Prefix>{cp}</Prefix>
      </CommonPrefixes>
      )}
      }
      <KeyCount>{contents.length}</KeyCount>
      <MaxKeys>1000</MaxKeys>
      <IsTruncated>{isTruncated}</IsTruncated>
        {contents.map(content =>
        <Contents>
          <Key>{content.key}</Key>
          <LastModified>{content.lastModified.toString}Z</LastModified>
          <ETag>{content.md5}</ETag>
          <Size>{content.size}</Size>
          <StorageClass>{content.storageClass}</StorageClass>
        </Contents>
        )}
    </ListBucketResult>
}
