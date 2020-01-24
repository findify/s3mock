name := "s3mock"

version := "0.3.0"

organization := "com.hiya"

scalaVersion := "2.12.8"

crossScalaVersions := Seq("2.11.12", "2.12.8", "2.13.1")

val akkaVersion = "2.5.11"

homepage := Some(url("https://github.com/hiyainc-oss/s3mock"))

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % "10.1.0",
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % "test",
  "org.scala-lang.modules" %% "scala-xml" % "1.1.0",
  "com.github.pathikrit" %% "better-files" % "3.4.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.8.0",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.294",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "test",
  "org.iq80.leveldb" % "leveldb" % "0.10",
  "com.lightbend.akka" %% "akka-stream-alpakka-s3" % "0.17" % "test"
)

parallelExecution in Test := false



licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
bintrayRepository := "maven"
bintrayOrganization := Some("hiyainc-oss")
bintrayReleaseOnPublish in ThisBuild := false
resolvers += Resolver.bintrayRepo("hiyainc-oss", "maven")
bintrayPackageLabels := Seq("scala", "collection")
