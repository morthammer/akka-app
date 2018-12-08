name := "basement_0.1"

version := "0.1"

scalaVersion := "2.12.2"

lazy val akkaVersion = "2.5.3"

//start with new h2 datastore
cleanFiles <+= baseDirectory { _ => new File("/tmp/h2/test") }

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % "10.0.10",
  "ch.qos.logback" % "logback-classic" % "1.0.9",
  "org.scalaz" %% "scalaz-core" % "7.2.18",
  "com.github.nscala-time" %% "nscala-time" % "2.18.0",
  "com.fasterxml.jackson.module" % "jackson-module-scala_2.12" % "2.9.2",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.0-RC1",
  "com.typesafe.slick" %% "slick" % "3.2.1",
  "com.h2database" % "h2" % "1.4.196",
  "net.databinder.dispatch" %% "dispatch-core" % "0.13.3",
  "org.json4s" %% "json4s-jackson" % "3.6.0-M2",
  "org.typelevel" %% "cats-core" % "1.3.1",
  "org.typelevel" %% "cats-effect" % "1.0.0",
  "org.mockito" % "mockito-all" % "1.9.0" % "test",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.0.10"
)
