package io.findify.s3mock.error


case class InternalErrorException(throwable: Throwable) extends Exception(s"Internal server error", throwable) {
  def toXML =
    <Error>
      <Code>InternalError</Code>
      <Message>{throwable.getMessage}</Message>
    </Error>
}
