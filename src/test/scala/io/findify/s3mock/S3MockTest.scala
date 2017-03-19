package io.findify.s3mock

import java.util.UUID

import better.files.File
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.transfer.{TransferManager, TransferManagerBuilder}
import io.findify.s3mock.provider.{FileProvider, InMemoryProvider}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.io.Source

/**
  * Created by shutty on 8/9/16.
  */
trait S3MockTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  private val workDir = s"/tmp/${UUID.randomUUID()}"
  private val fileBasedS3 = new AmazonS3Client(new BasicAWSCredentials("hello", "world"))
  private val fileBasedPort = 8001
  private val fileBasedServer = new S3Mock(fileBasedPort, new FileProvider(workDir))
  private val fileBasedTransferManager: TransferManager = TransferManagerBuilder.standard().withS3Client(fileBasedS3).build()
  fileBasedS3.setEndpoint(s"http://127.0.0.1:$fileBasedPort")

  private val inMemoryS3 = new AmazonS3Client(new BasicAWSCredentials("hello", "world"))
  private val inMemoryPort = 8002
  private val inMemoryServer = new S3Mock(inMemoryPort, new InMemoryProvider)
  private val inMemoryTransferManager: TransferManager = TransferManagerBuilder.standard().withS3Client(inMemoryS3).build()
  inMemoryS3.setEndpoint(s"http://127.0.0.1:$inMemoryPort")

  case class Fixture(server: S3Mock, client: AmazonS3Client, tm: TransferManager, name: String, port: Int)
  val fixtures = List(Fixture(fileBasedServer, fileBasedS3, fileBasedTransferManager, "file based S3Mock", fileBasedPort),
    Fixture(inMemoryServer, inMemoryS3, inMemoryTransferManager, "in-memory S3Mock", inMemoryPort))

  def behaviour(fixture: => Fixture) : Unit

  for (fixture <- fixtures) {
    fixture.name should behave like behaviour(fixture)
  }

  override def beforeAll = {
    if (!File(workDir).exists) File(workDir).createDirectory()
    fileBasedServer.start
    inMemoryServer.start
    super.beforeAll
  }
  override def afterAll = {
    super.afterAll
    inMemoryServer.stop
    fileBasedServer.stop
    inMemoryTransferManager.shutdownNow()
    File(workDir).delete()
  }
  def getContent(s3Object: S3Object): String = Source.fromInputStream(s3Object.getObjectContent, "UTF-8").mkString

}

