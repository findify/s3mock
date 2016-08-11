package io.findify.s3mock.error

/**
  * Created by shutty on 8/11/16.
  */
case class NoSuchBucketException(bucket:String) extends Exception(s"bucket does not exist: s3://$bucket")
