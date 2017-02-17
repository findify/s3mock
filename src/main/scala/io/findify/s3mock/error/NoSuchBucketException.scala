package io.findify.s3mock.error

/**
  * Created by shutty on 8/11/16.
  */
case class NoSuchBucketException(bucket:String) extends Exception(s"bucket does not exist: s3://$bucket") {
  def toXML = <Error>
    <Code>NoSuchBucket</Code>
    <Message>The specified bucket does not exist</Message>
    <BucketName>{bucket}</BucketName>
  </Error>
}
