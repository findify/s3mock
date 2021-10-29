name := "s3mock"

version := "0.2.6"

organization := "io.findify"

scalaVersion in ThisBuild := "2.13.2"

crossScalaVersions in ThisBuild := Seq("2.12.13","2.13.2")

val akkaVersion = "2.6.17"

licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT"))

homepage := Some(url("https://github.com/findify/s3mock"))

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % "10.1.11",
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % "test",
  "org.scala-lang.modules" %% "scala-xml" % "2.0.1",
  "org.scala-lang.modules" %% "scala-collection-compat" % "2.5.0",
  "com.github.pathikrit" %% "better-files" % "3.9.1",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.12.90",
  "org.scalatest" %% "scalatest" % "3.2.9" % "test",
  "ch.qos.logback" % "logback-classic" % "1.2.6" % "test",
  "org.iq80.leveldb" % "leveldb" % "0.12",
  "com.lightbend.akka" %% "akka-stream-alpakka-s3" % "3.0.3" % "test",
  "javax.xml.bind" % "jaxb-api" % "2.3.1",
  "com.sun.xml.bind" % "jaxb-core" % "3.0.1",
  "com.sun.xml.bind" % "jaxb-impl" % "3.0.1"
)

libraryDependencies ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, major)) if major >= 13 =>
      Seq("org.scala-lang.modules" %% "scala-parallel-collections" % "0.2.0" % "test")
    case _ =>
      Seq()
  }
}

parallelExecution in Test := false

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomExtra := (
    <scm>
      <url>git@github.com:findify/s3mock.git</url>
      <connection>scm:git:git@github.com:findify/s3mock.git</connection>
    </scm>
    <developers>
      <developer>
        <id>romangrebennikov</id>
        <name>Roman Grebennikov</name>
        <url>http://www.dfdx.me</url>
      </developer>
    </developers>)

enablePlugins(DockerPlugin)
assemblyJarName in assembly := "s3mock.jar"
mainClass in assembly := Some("io.findify.s3mock.Main")
test in assembly := {}

dockerfile in docker := new Dockerfile {
  from("adoptopenjdk/openjdk11:jre-11.0.7_10-debian")
  expose(8001)
  add(assembly.value, "/app/s3mock.jar")
  entryPoint(
      "java", 
      "-Xmx128m", 
      "-jar", 
      "--add-opens",
      "java.base/jdk.internal.ref=ALL-UNNAMED",
      "/app/s3mock.jar"
  )
}
imageNames in docker := Seq(
  ImageName(s"findify/s3mock:${version.value.replaceAll("\\+", "_")}"),
  ImageName(s"findify/s3mock:latest")
)

publishTo := sonatypePublishToBundle.value