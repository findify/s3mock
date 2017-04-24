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

Not supported features (these might be implemented later):
* authentication: s3proxy will accept any credentials without validity checking
* bucket policy, ACL, versioning
* object ACL, batch delete

## Installation

s3mock package is available for Scala 2.11/2.12 (on Java 8). To install using SBT, add these
 statements to your `build.sbt`:

    libraryDependencies += "io.findify" %% "s3mock" % "0.2.0" % "test",

On maven, update your `pom.xml` in the following way:
```xml
    // add this entry to <dependencies/>
    <dependency>
        <groupId>io.findify</groupId>
        <artifactId>s3mock_2.12</artifactId>
        <version>0.2.0</version>
        <type>pom</type>
        <scope>test</scope>
    </dependency>
```
## Usage
Scala:
```scala
    import io.findify.s3mock.S3Mock
    import com.amazonaws.auth.AnonymousAWSCredentials
    import com.amazonaws.services.s3.AmazonS3Client
    
    // create and start S3 API mock
    val api = S3Mock(port = 8001, dir = "/tmp/s3")
    api.start

    // AWS S3 client setup
    val credentials = new AnonymousAWSCredentials()
    val client = new AmazonS3Client(credentials)
    // use IP for endpoint address as AWS S3 SDK uses DNS-based bucket access scheme
    // resulting in attempts to connect to addresses like "bucketname.localhost"
    // which requires specific DNS setup
    client.setEndpoint("http://127.0.0.1:8001")

    // use it as usual
    client.createBucket("foo")
    client.putObject("foo", "bar", "baz")
```
Java:
```java
    import io.findify.s3mock.S3Mock;
    import com.amazonaws.auth.AnonymousAWSCredentials;
    import com.amazonaws.services.s3.AmazonS3Client;

    S3Mock api = S3Mock.create(8001, "/tmp/s3");
    api.start();
            
    AmazonS3Client client = new AmazonS3Client(new AnonymousAWSCredentials());
    // use IP endpoint to override DNS-based bucket addressing
    client.setEndpoint("http://127.0.0.1:8001");
    client.createBucket("testbucket");
    client.putObject("testbucket", "file/name", "contents");
```
    
## License

The MIT License (MIT)

Copyright (c) 2016 Findify AB

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.