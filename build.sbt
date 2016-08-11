name := "s3mock"

version := "0.0.7"

organization := "io.findify"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.4.8",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.8",
  "com.typesafe.akka" %% "akka-http-experimental" % "2.4.8",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.5",
  "joda-time" % "joda-time" % "2.9.3",
  "com.github.pathikrit" %% "better-files" % "2.16.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.24" % "test",
  "commons-io" % "commons-io" % "2.5"
)

licenses += ("MIT", url("https://opensource.org/licenses/MIT"))

bintrayOrganization := Some("findify")

parallelExecution in Test := false