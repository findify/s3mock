package io.findify.s3mock.response

import java.time.Instant

case class Part(partNo: Int, etag: String, size: Long, lastModified: Instant) {
  def toXML =
    <Part>
      <PartNumber>{partNo}</PartNumber>
      <ETag>{etag}</ETag>
      <Size>{size}</Size>
      <LastModified>{lastModified}</LastModified>
    </Part>
}

case class ListParts(bucket: String,
                     key: String,
                     uploadId: String,
                     marker: Int,
                     nextMarker: Option[Int],
                     maxParts: Int,
                     parts: List[Part]) {
  def toXml =
    <ListPartsOutput xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
      <Bucket>{bucket}</Bucket>
      <Key>{key}</Key>
      <UploadId>{uploadId}</UploadId>
      <PartNumberMarker>{marker}</PartNumberMarker>
      {nextMarker.map(partNo => <NextPartNumberMarker>{partNo}</NextPartNumberMarker>).toSeq}
      <MaxParts>{maxParts}</MaxParts>
      <IsTruncated>{nextMarker.isDefined}</IsTruncated>
      {parts.map(_.toXML)}
    </ListPartsOutput>
}
