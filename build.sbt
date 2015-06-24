import play.PlayScala

scalaVersion := "2.11.6"

name := """website"""

version := "0.1-SNAPSHOT"

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(  javaJdbc,
  cache,
  filters,
  javaWs,
  "com.google.inject" % "guice" % "3.0",
  "javax.inject" % "javax.inject" % "1",
  "org.reactivemongo" %% "reactivemongo" % "0.10.5.akka23-SNAPSHOT",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.5.akka23-SNAPSHOT",
  "org.mockito" % "mockito-core" % "1.9.5" % "test",
  "com.sksamuel.scrimage" %% "scrimage-core" % "1.4.2",
  "com.github.t3hnar" % "scala-bcrypt_2.10" % "2.3",
  "com.typesafe.play.plugins" %% "play-plugins-mailer" % "2.3.1"
)

lazy val main = (project in file(".")).enablePlugins(PlayScala)
