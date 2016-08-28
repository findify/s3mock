name := "s3mock"

version := "0.1.1"

organization := "io.findify"

scalaVersion := "2.11.8"

val akkaVersion = "2.4.9"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-experimental" % akkaVersion,
  "org.scala-lang.modules" %% "scala-xml" % "1.0.5",
  "joda-time" % "joda-time" % "2.9.3",
  "com.github.pathikrit" %% "better-files" % "2.16.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.24" % "test",
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % "test",
  //"com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "commons-io" % "commons-io" % "2.5" % "test",
  "ch.qos.logback" % "logback-classic" % "1.1.7" % "test"
)

licenses += ("MIT", url("https://opensource.org/licenses/MIT"))

bintrayOrganization := Some("findify")

parallelExecution in Test := false