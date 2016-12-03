name := "s3mock"

version := "0.1.5"

organization := "io.findify"

scalaVersion := "2.11.8"

val akkaVersion = "2.4.14"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % "10.0.0",
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % "test",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
  "com.github.pathikrit" %% "better-files" % "2.16.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.63",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "commons-io" % "commons-io" % "2.5" % "test",
  "ch.qos.logback" % "logback-classic" % "1.1.7" % "test"
)

licenses += ("MIT", url("https://opensource.org/licenses/MIT"))

bintrayOrganization := Some("findify")

parallelExecution in Test := false