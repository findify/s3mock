package io.findify.s3mock.error

case class NoSuchUploadException(bucket: String, key: String, uploadId: String)
    extends Exception(s"upload id does not exist: $uploadId") {
  def toXML =
    <Error>
      <Code>NoSuchUpload</Code>
      <Message>The specified multipart upload does not exist</Message>
      <Resource>/{bucket}/{key}</Resource>
    </Error>
}
