# S3 mock library for Java/Scala

[![Build Status](https://travis-ci.org/findify/s3mock.svg?branch=master)](https://travis-ci.org/findify/s3mock)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.findify/s3mock_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.findify/s3mock_2.12)

s3mock is a web service implementing AWS S3 API, which can be used for local testing of your code using S3
but without hitting real S3 endpoints.

Implemented API methods:
* list buckets
* list objects (all & by prefix)
* create bucket
* delete bucket
* put object (via PUT, POST, multipart and chunked uploads are also supported)
* copy object
* get object
* delete object
* batch delete

Not supported features (these might be implemented later):
* authentication: s3proxy will accept any credentials without validity and signature checking
* bucket policy, ACL, versioning
* object ACL
* posix-incompatible key structure with file-based provider, for example keys `/some.dir/file.txt` and `/some.dir` in the same bucket

## Installation

s3mock package is available for Scala 2.11/2.12 (on Java 8). To install using SBT, add these
 statements to your `build.sbt`:

    libraryDependencies += "io.findify" %% "s3mock" % "0.2.4" % "test",

On maven, update your `pom.xml` in the following way:
```xml
    // add this entry to <dependencies/>
    <dependency>
        <groupId>io.findify</groupId>
        <artifactId>s3mock_2.12</artifactId>
        <version>0.2.4</version>
        <scope>test</scope>
    </dependency>
```

S3Mock is also available as a [docker container](https://hub.docker.com/r/findify/s3mock/) for out-of-jvm testing:
```bash
docker run -p 8001:8001 findify/s3mock:latest
```

## Usage

Just point your s3 client to a localhost, enable path-style access, and it should work out of the box.

There are two working modes for s3mock:
* File-based: it will map a local directory as a collection of s3 buckets. This mode can be useful when you need to have a bucket with some pre-loaded data (and too lazy to re-upload everything on each run).
* In-memory: keep everything in RAM. All the data you've uploaded to s3mock will be wiped completely on shutdown. 

Java:
```java
    import com.amazonaws.auth.AWSStaticCredentialsProvider;
    import com.amazonaws.auth.AnonymousAWSCredentials;
    import com.amazonaws.client.builder.AwsClientBuilder;
    import com.amazonaws.services.s3.AmazonS3;
    import com.amazonaws.services.s3.AmazonS3Builder;
    import com.amazonaws.services.s3.AmazonS3Client;
    import com.amazonaws.services.s3.AmazonS3ClientBuilder;
    import io.findify.s3mock.S3Mock;
    
    /*
     S3Mock.create(8001, "/tmp/s3");
     */
    S3Mock api = new S3Mock.Builder().withPort(8001).withInMemoryBackend().build();
    api.start();
            
    /* AWS S3 client setup.
     *  withPathStyleAccessEnabled(true) trick is required to overcome S3 default 
     *  DNS-based bucket access scheme
     *  resulting in attempts to connect to addresses like "bucketname.localhost"
     *  which requires specific DNS setup.
     */
    EndpointConfiguration endpoint = new EndpointConfiguration("http://localhost:8001", "us-west-2");
    AmazonS3Client client = AmazonS3ClientBuilder
      .standard()
      .withPathStyleAccessEnabled(true)  
      .withEndpointConfiguration(endpoint)
      .withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))     
      .build();

    client.createBucket("testbucket");
    client.putObject("testbucket", "file/name", "contents");
    api.stop();
```

Scala with AWS S3 SDK:
```scala
    import com.amazonaws.auth.AWSStaticCredentialsProvider
    import com.amazonaws.auth.AnonymousAWSCredentials
    import com.amazonaws.client.builder.AwsClientBuilder
    import com.amazonaws.services.s3.AmazonS3
    import com.amazonaws.services.s3.AmazonS3Builder
    import com.amazonaws.services.s3.AmazonS3Client
    import com.amazonaws.services.s3.AmazonS3ClientBuilder
    import io.findify.s3mock.S3Mock

    
    /** Create and start S3 API mock. */
    val api = S3Mock(port = 8001, dir = "/tmp/s3")
    api.start

    /* AWS S3 client setup.
     *  withPathStyleAccessEnabled(true) trick is required to overcome S3 default 
     *  DNS-based bucket access scheme
     *  resulting in attempts to connect to addresses like "bucketname.localhost"
     *  which requires specific DNS setup.
     */
    val endpoint = new EndpointConfiguration("http://localhost:8001", "us-west-2")
    val client = AmazonS3ClientBuilder
      .standard
      .withPathStyleAccessEnabled(true)  
      .withEndpointConfiguration(endpoint)
      .withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))     
      .build

    /** Use it as usual. */
    client.createBucket("foo")
    client.putObject("foo", "bar", "baz")
```

Scala with Alpakka 0.8:
```scala
    import akka.actor.ActorSystem
    import akka.stream.ActorMaterializer
    import akka.stream.alpakka.s3.scaladsl.S3Client
    import akka.stream.scaladsl.Sink
    import com.typesafe.config.ConfigFactory
    import scala.collection.JavaConverters._

    val config = ConfigFactory.parseMap(Map(
      "akka.stream.alpakka.s3.proxy.host" -> "localhost",
      "akka.stream.alpakka.s3.proxy.port" -> 8001,
      "akka.stream.alpakka.s3.proxy.secure" -> false,
      "akka.stream.alpakka.s3.path-style-access" -> true
    ).asJava)
    implicit val system = ActorSystem.create("test", config)
    implicit val mat = ActorMaterializer()
    import system.dispatcher
    val s3a = S3Client()
    val contents = s3a.download("bucket", "key").runWith(Sink.reduce[ByteString](_ ++ _)).map(_.utf8String)
      
```
    
## License

The MIT License (MIT)

Copyright (c) 2016 Findify AB

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
