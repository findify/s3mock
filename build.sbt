name := "s3mock"

version := "0.1.7"

organization := "io.findify"

scalaVersion := "2.11.8"

crossScalaVersions := Seq("2.11.8", "2.12.1")

val akkaVersion = "2.4.17"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % "10.0.4",
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % "test",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
  "com.github.pathikrit" %% "better-files" % "2.17.1",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.104",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "ch.qos.logback" % "logback-classic" % "1.2.1" % "test",
  "org.iq80.leveldb" % "leveldb" % "0.9"
)

licenses += ("MIT", url("https://opensource.org/licenses/MIT"))

bintrayOrganization := Some("findify")

parallelExecution in Test := false