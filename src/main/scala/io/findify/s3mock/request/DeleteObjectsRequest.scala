package io.findify.s3mock.request

/**
  * Created by shutty on 3/13/17.
  */

case class DeleteObjectsRequest(objects: Seq[String])

object DeleteObjectsRequest {
  def apply(node: scala.xml.Node) = {
    val objs = (node \ "Object").map(_ \ "Key").map(_.text)
    new DeleteObjectsRequest(objs)
  }
}
