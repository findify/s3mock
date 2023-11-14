package io.findify.s3mock.response

import org.apache.pekko.http.scaladsl.model.DateTime


/**
  * Created by shutty on 12/3/16.
  */
case class CopyObjectResult(lastModified: DateTime, etag: String) {
  def toXML =
    <CopyObjectResult>
      <LastModified>{lastModified.toString}Z</LastModified>
      <ETag>"{etag}"</ETag>
    </CopyObjectResult>
}
