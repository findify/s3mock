logLevel := Level.Warn

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.5.1")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.2")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.1")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.13")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10")
addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.5.0")
addDependencyTreePlugin