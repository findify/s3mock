0.2.1 (not released)
=======
* Bump akka to 2.5.1
* fix issue when DeleteObjects response was malformed for multi-object deletes
* alpakka support test case

0.2.0
=======
* Support for ranged get requests ([#39](https://github.com/findify/s3mock/pull/39))
* In-memory backend ([#37](https://github.com/findify/s3mock/pull/37))
* Bugfix: ObjectListing#getCommonPrefixes order is not alphabetical ([#41](https://github.com/findify/s3mock/issues/41))
* Akka 2.5.0 support