package io.findify.s3mock.request

/**
  * Created by shutty on 8/10/16.
  */
case class CreateBucketConfiguration(locationConstraint:Option[String])

object CreateBucketConfiguration {
  def apply(xml:scala.xml.Node) = {
    val region = xml.find(_.label == "locationConstraint").map(_.text)
    new CreateBucketConfiguration(region)
  }
}
