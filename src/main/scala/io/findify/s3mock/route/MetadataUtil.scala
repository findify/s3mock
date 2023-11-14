package io.findify.s3mock.route

import java.lang.Iterable
import java.util

import org.apache.pekko.http.javadsl.model.HttpHeader
import org.apache.pekko.http.scaladsl.model.HttpRequest
import com.amazonaws.AmazonClientException
import com.amazonaws.services.s3.Headers
import com.amazonaws.services.s3.internal.ServiceUtils
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.util.{DateUtils, StringUtils}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._

object MetadataUtil extends LazyLogging {

   def populateObjectMetadata(request: HttpRequest): ObjectMetadata = {
    val metadata = new ObjectMetadata()
    val ignoredHeaders: util.HashSet[String] = new util.HashSet[String]()
    ignoredHeaders.add(Headers.DATE)
    ignoredHeaders.add(Headers.SERVER)
    ignoredHeaders.add(Headers.REQUEST_ID)
    ignoredHeaders.add(Headers.EXTENDED_REQUEST_ID)
    ignoredHeaders.add(Headers.CLOUD_FRONT_ID)
    ignoredHeaders.add(Headers.CONNECTION)

    val headers: Iterable[HttpHeader] = request.getHeaders()
    for (header <- headers.asScala) {
      var key: String = header.name()
      if (StringUtils.beginsWithIgnoreCase(key, Headers.S3_USER_METADATA_PREFIX)) {
        key = key.substring(Headers.S3_USER_METADATA_PREFIX.length)
        metadata.addUserMetadata(key, header.value())
      }
      //      else if (ignoredHeaders.contains(key)) {
      // ignore...
      //      }
      else if (key.equalsIgnoreCase(Headers.LAST_MODIFIED)) try
        metadata.setHeader(key, ServiceUtils.parseRfc822Date(header.value()))

      catch {
        case pe: Exception => logger.warn("Unable to parse last modified date: " + header.value(), pe)
      }
      else if (key.equalsIgnoreCase(Headers.CONTENT_LENGTH)) try
        metadata.setHeader(key, java.lang.Long.parseLong(header.value()))

      catch {
        case nfe: NumberFormatException => throw new AmazonClientException("Unable to parse content length. Header 'Content-Length' has corrupted data" + nfe.getMessage, nfe)
      }
      else if (key.equalsIgnoreCase(Headers.ETAG)) metadata.setHeader(key, ServiceUtils.removeQuotes(header.value()))
      else if (key.equalsIgnoreCase(Headers.EXPIRES)) try
        metadata.setHttpExpiresDate(DateUtils.parseRFC822Date(header.value()))

      catch {
        case pe: Exception => logger.warn("Unable to parse http expiration date: " + header.value(), pe)
      }
      //      else if (key.equalsIgnoreCase(Headers.EXPIRATION)) new ObjectExpirationHeaderHandler[ObjectMetadata]().handle(metadata, response)
      //      else if (key.equalsIgnoreCase(Headers.RESTORE)) new ObjectRestoreHeaderHandler[ObjectRestoreResult]().handle(metadata, response)
      //      else if (key.equalsIgnoreCase(Headers.REQUESTER_CHARGED_HEADER)) new S3RequesterChargedHeaderHandler[S3RequesterChargedResult]().handle(metadata, response)
      else if (key.equalsIgnoreCase(Headers.S3_PARTS_COUNT)) try
        metadata.setHeader(key, header.value().toInt)

      catch {
        case nfe: NumberFormatException => throw new AmazonClientException("Unable to parse part count. Header x-amz-mp-parts-count has corrupted data" + nfe.getMessage, nfe)
      }
      else metadata.setHeader(key, header.value())
    }

    if(metadata.getContentType == null){
      metadata.setContentType(request.entity.getContentType.toString)
    }
    metadata
  }
}
