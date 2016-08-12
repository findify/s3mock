# S3 mock library for Java/Scala

[![Build Status](https://travis-ci.org/shuttie/s3mock.svg?branch=master)](https://travis-ci.org/shuttie/sqsmock)

Sqsmock is a web service implementing AWS S3 API, which can be used for local testing of your code using S3
but without hitting real S3 endpoints.

Implemented API methods:
* list buckets
* list objects (all & by prefix)
* create bucket
* delete bucket
* put object (via PUT, POST, multipart and chunked uploads are also supported)
* get object
* delete object

Not supported features (these might be implemented later):
* authentication: s3proxy will accept any credentials without validity checning
* bucket policy, ACL, versioning
* object ACL, batch delete

## Installation

Sqsmock package is available for Scala 2.11 (on Java 8). To install using SBT, add these
 statements to your `build.sbt`:

    resolvers += Resolver.bintrayRepo("findify", "maven")
    libraryDependencies += "io.findify" %% "s3mock" % "0.0.17" % "test",

On maven, update your `pom.xml` in the following way:

    // add this entry to <repositories/>
    <repository>
      <id>findify</id>
      <url>https://dl.bintray.com/findify/maven/</url>
    </repository>

    // add this entry to <dependencies/>
    <dependency>
        <groupId>io.findify</groupId>
        <artifactId>sqsmock_2.11</artifactId>
        <version>0.0.17</version>
        <type>pom</type>
        <scope>test</scope>
    </dependency>

## Usage
Scala:

    // create and start S3 API mock
    val api = new S3Mock(port = 8001, provider = new FileProvider("/tmp/s3"))
    api.start()

    // AWS SQS client setup
    val credentials = new AnonymousAWSCredentials()
    val client = new AmazonS3Client(credentials)
    client.setEndpoint("http://localhost:8001")

    // use it as usual
    val queue = client.createBucket("foo")
    client.putObject("foo", "bar", "baz")

Java:

    // TODO
    
## License

The MIT License (MIT)

Copyright (c) 2016 Findify AB

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.