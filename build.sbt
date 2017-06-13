import Git._
import Release._

val kafkaVersion = "0.10.2.1"

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
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(BuildInfoPlugin, GitVersioning, GitBranchPrompt)
  .settings(
    organization := "com.sky",
    scalaVersion := "2.12.1",
    name := "kafka-configurator",
    libraryDependencies ++= dependencies,
    resolvers += Resolver.bintrayRepo("cakesolutions", "maven"),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
    scalacOptions += "-language:implicitConversions",
    fork in run := true,
    gitSettings,
    releaseSettings
  )

// TODO: Move these into Release.scala
// Useful tasks to show what versions would be used if a release was done.
val showReleaseVersion = taskKey[String]("the future version once releaseNextVersion has been applied to it")
val showNextVersion = taskKey[String]("the future version once releaseNextVersion has been applied to it")
showReleaseVersion := { val rV = releaseVersion.value.apply(version.value); println(rV); rV }
showNextVersion := { val nV = releaseNextVersion.value.apply(version.value); println(nV); nV }
