0.2.6
=======
* scala 2.13 support
* java 11 support

0.2.5
=======
* multipart copy support
* fix issue with % char on multipart alpakka uploads
* add JAXB module on java 9 docker container
* fix OverlappingFileLockException in GetObject ([#83](https://github.com/findify/s3mock/issues/83))
* docker: allow serving a volume-mounted directory
* fix s3mock not correctly shutting down during internal ActorSystem being active ([#67](https://github.com/findify/s3mock/issues/67))
* bump better-files dependency to 3.x ([#17](https://github.com/findify/s3mock/issues/17))
* migrate build to sbt 1.1, use akka-http 10.1.x

0.2.4
=======
* pom -> jar dependency type doc fix
* support alpakka multipart uploads
* support alpakka listObjects ([#66](https://github.com/findify/s3mock/issues/66))
* fix bug with etag on FileProvider being alsays "0" ([#70](https://github.com/findify/s3mock/issues/70))
* fix last-modified header always being equal to "1970-01-01 00:00:00"([65](https://github.com/findify/s3mock/issues/70))
* wrong content-type for listObjects ([#60](https://github.com/findify/s3mock/issues/60))
* deleteObjects broken on aws s3 sdk 2.0 ([#71](https://github.com/findify/s3mock/issues/60))
* docker image for non-jvm tests

0.2.3
=======
* windows compatibility in FileProvider ([#28](https://github.com/findify/s3mock/issues/28))
* Max Keys not respected when calling list objects (V2) ([#47](https://github.com/findify/s3mock/issues/47))
* getETag from getObjectMetadata returns null ([#48](https://github.com/findify/s3mock/issues/48))
* update to akka 2.5.2, akka-http 10.0.7
* fix concurrent requests causing weird locking issues on FileProvider ([#52](https://github.com/findify/s3mock/issues/52))
* fix warnings in GetObject about incorrent headers ([#54](https://github.com/findify/s3mock/issues/54))

0.2.2
=======
* More convenient and traditional Java API with Builder-style instance creation
* Docs update for alpakka usage
* Javadocs for all public API methods
* use latest aws-java-sdk-s3 library

0.2.1
=======
* Bump akka to 2.5.1
* fix issue when DeleteObjects response was malformed for multi-object deletes
* alpakka support test case
* fix subpath get/delete issues [#45](https://github.com/findify/s3mock/issues/45)

0.2.0
=======
* Support for ranged get requests ([#39](https://github.com/findify/s3mock/pull/39))
* In-memory backend ([#37](https://github.com/findify/s3mock/pull/37))
* Bugfix: ObjectListing#getCommonPrefixes order is not alphabetical ([#41](https://github.com/findify/s3mock/issues/41))
* Akka 2.5.0 support