package io.findify.s3mock.error

/**
  * Created by shutty on 8/11/16.
  */
case class NoSuchKeyException(bucket:String, key:String) extends Exception(s"key does not exist: s3://$bucket/$key") {
  def toXML =
    <Error>
      <Code>NoSuchKey</Code>
      <Message>The resource you requested does not exist</Message>
      <Resource>/{bucket}/{key}</Resource>
    </Error>
}
