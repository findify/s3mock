package io.findify.s3mock.request

/**
  * Created by shutty on 8/10/16.
  */
case class CompleteMultipartUploadPart(partNumber:Int, etag:String)
case class CompleteMultipartUpload(parts:List[CompleteMultipartUploadPart])

object CompleteMultipartUploadPart {
  def apply(node: scala.xml.Node) = new CompleteMultipartUploadPart(
    partNumber = (node \ "PartNumber").text.toInt,
    etag = (node \ "ETag").text
  )
}

object CompleteMultipartUpload {
  def apply(node:scala.xml.Node) = {
    val child = node \ "Part"
    new CompleteMultipartUpload(
      parts = child.map(n => CompleteMultipartUploadPart(n)).toList
    )
  }
}