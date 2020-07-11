package io.findify.s3mock.error

/**
  * Created by furkilic on 7/11/20.
  */
case class NoSuchVersionException(bucket:String, key:String, versionId:String) extends Exception(s"version does not exist: s3://$bucket/$key/$versionId") {
  def toXML =
    <Error>
      <Code>NoSuchVersion</Code>
      <Message>The resource you requested does not exist</Message>
      <Resource>/{bucket}/{key}/{versionId}</Resource>
    </Error>
}
