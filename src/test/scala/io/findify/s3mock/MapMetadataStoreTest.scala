package io.findify.s3mock

import java.util

import com.amazonaws.services.s3.model.ObjectMetadata
import io.findify.s3mock.provider.metadata.MapMetadataStore
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.collection.JavaConverters._
/**
  * Created by shutty on 3/13/17.
  */
class MapMetadataStoreTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  val mm = new MapMetadataStore("/tmp/s3")

  "map metadata store" should "save md to a fresh store" in {
    val meta = new ObjectMetadata()
    val user = new util.HashMap[String,String]()
    user.put("foo", "bar")
    meta.setUserMetadata(user)
    mm.put("foo", "bar", meta)
    val m2 = mm.get("foo", "bar").get
    m2.getUserMetadata shouldBe meta.getUserMetadata
  }
}
