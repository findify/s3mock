package io.findify.s3mock.response

/**
  * Created by shutty on 3/13/17.
  */
case class DeleteObjectsResponse(deleted: Seq[String], error: Seq[String]) {
  def toXML = {
    <DeleteResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
        { deleted.map(d => <Deleted><Key>{d}</Key></Deleted>) }
      { if (error.nonEmpty) {
      <Error>
        { error.map(e => {
        <Key>{e}</Key>
          <Code>InternalError</Code>
          <Message>Cannot delete</Message>
      })}
      </Error>
      }}
    </DeleteResult>
  }
}
