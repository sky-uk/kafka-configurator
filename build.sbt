import Aliases._
import Bintray._
import BuildInfo._
import Release._
import Docker._

val kafkaVersion = "1.1.0"

val kafkaDeps = Seq(
  "org.apache.kafka" % "kafka-clients",
  "org.apache.kafka" %% "kafka"
).map(_ % kafkaVersion)

val dependencies = Seq(
  "com.github.scopt" %% "scopt" % "3.5.0",
  "org.zalando" %% "grafter" % "1.6.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "io.circe" %% "circe-yaml" % "0.6.1",
  "io.circe" %% "circe-generic" % "0.8.0",

  "org.scalatest" %% "scalatest" % "3.0.1" % Test,
  "net.cakesolutions" %% "scala-kafka-client-testkit" % kafkaVersion % Test,
  "org.mockito" % "mockito-all" % "1.10.19" % Test
) ++ kafkaDeps

val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin, JavaAppPackaging, UniversalDeployPlugin, DockerPlugin)
  .settings(
    defineCommandAliases,
    organization := "com.sky",
    scalaVersion := "2.12.1",
    name := "kafka-configurator",
    libraryDependencies ++= dependencies,
    resolvers += Resolver.bintrayRepo("cakesolutions", "maven"),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
    scalacOptions += "-language:implicitConversions",
    fork in run := true,
    buildInfoSettings,
    releaseSettings,
    bintraySettings,
    dockerSettings
  )
