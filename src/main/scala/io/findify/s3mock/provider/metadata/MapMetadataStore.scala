package io.findify.s3mock.provider.metadata

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import better.files.File
import com.amazonaws.services.s3.model.ObjectMetadata
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory._
import org.iq80.leveldb._
import org.iq80.leveldb.impl.Iq80DBFactory

import scala.collection.mutable

/**
  * Created by shutty on 3/13/17.
  */
class MapMetadataStore(path: String) extends MetadataStore {
  val bucketMetadata = mutable.Map[String,DB]()


  override def put(bucket: String, key: String, meta: ObjectMetadata): Unit = {
    val map = load(path, bucket)
    map.put(bytes(key), meta2bytes(meta))
  }
  override def get(bucket: String, key: String): Option[ObjectMetadata] = {
    val map = load(path, bucket)
    val meta = Option(map.get(bytes(key))).map(bytes2meta)
    meta
  }
  override def delete(bucket: String, key: String): Unit = {
    val map = load(path, bucket)
    map.delete(bytes(key))
  }

  override def remove(bucket: String): Unit = {
    bucketMetadata.get(bucket).foreach(db => {
      db.close()
      bucketMetadata.remove(bucket)
    })
    val file = File(s"$path/$bucket.metadata")
    if (file.exists) file.delete()
  }

  private def load(path: String, bucket: String): DB = synchronized {
    bucketMetadata.get(bucket) match {
      case Some(db) => db
      case None =>
        val options = new Options()
        options.createIfMissing(true)
        val db = Iq80DBFactory.factory.open(File(s"$path/$bucket.metadata").toJava, options)
        bucketMetadata.put(bucket, db)
        db
    }
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
