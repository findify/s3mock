package io.findify.s3mock

import java.util

import com.amazonaws.services.s3.model.ObjectMetadata
import io.findify.s3mock.provider.metadata.{InMemoryMetadataStore, MapMetadataStore, MetadataStore}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
/**
  * Created by shutty on 3/13/17.
  */
class MapMetadataStoreTest extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  for (metadataStore <- List((new MapMetadataStore("/tmp/s3"), "MapMetadataStore"),
    (new InMemoryMetadataStore, "InMemoryMetadataStore"))) {
    metadataStore._2 should behave like mdStoreBehaviour(metadataStore._1)
  }

  def mdStoreBehaviour(mm: => MetadataStore) = {
    it should "save md to a fresh store" in {
      val meta = new ObjectMetadata()
      val user = new util.HashMap[String, String]()
      user.put("foo", "bar")
      meta.setUserMetadata(user)
      mm.put("foo", "bar", meta)
      val m2 = mm.get("foo", "bar").get
      m2.getUserMetadata shouldBe meta.getUserMetadata
    }
  }
}
