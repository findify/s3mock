package io.findify.s3mock.provider.metadata

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import better.files.File
import com.amazonaws.services.s3.model.ObjectMetadata
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory._
import org.iq80.leveldb._
import org.iq80.leveldb.impl.Iq80DBFactory

/**
  * Created by shutty on 3/13/17.
  */
class MapMetadataStore(path: String) extends MetadataStore {
  override def put(bucket: String, key: String, meta: ObjectMetadata): Unit = {
    val map = load(path, bucket)
    map.put(bytes(key), meta2bytes(meta))
    map.close()
  }
  override def get(bucket: String, key: String): Option[ObjectMetadata] = {
    val map = load(path, bucket)
    val meta = Option(map.get(bytes(key))).map(bytes2meta)
    map.close()
    meta
  }

  private def load(path: String, bucket: String): DB = {
    val options = new Options()
    options.createIfMissing(true)
    Iq80DBFactory.factory.open(File(s"$path/$bucket.metadata").toJava, options)
  }

  private def meta2bytes(meta: ObjectMetadata) = {
    val out = new ByteArrayOutputStream()
    val stream = new ObjectOutputStream(out)
    stream.writeObject(meta)
    stream.close()
    out.toByteArray
  }

  private def bytes2meta(bytes: Array[Byte]): ObjectMetadata = {
    val in = new ByteArrayInputStream(bytes)
    val stream = new ObjectInputStream(in)
    stream.readObject().asInstanceOf[ObjectMetadata]
  }
}
