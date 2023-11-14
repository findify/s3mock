package io.findify.s3mock.response

import org.apache.pekko.http.scaladsl.model.DateTime


/**
  * Created by shutty on 8/9/16.
  */
case class Bucket(name:String, creationDate:DateTime)
case class ListAllMyBuckets(ownerName:String, ownerUUID:String, buckets:List[Bucket]) {
  def toXML =
    <ListAllMyBucketsResult xmlns="http://s3.amazonaws.com/doc/2006-03-01">
      <Owner>
        <ID>{ownerUUID}</ID>
        <DisplayName>{ownerName}</DisplayName>
      </Owner>
      <Buckets>
        {
        buckets.map(bucket =>
          <Bucket>
            <Name>{bucket.name}</Name>
            <CreationDate>{bucket.creationDate.toString}Z</CreationDate>
          </Bucket>)
        }
      </Buckets>
    </ListAllMyBucketsResult>
}
