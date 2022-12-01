import Aliases._
import BuildInfo._
import Docker._

lazy val scmUrl = "https://github.com/sky-uk/kafka-configurator"

Global / onChangedBuildSource := ReloadOnSourceChanges
semanticdbEnabled             := true
semanticdbVersion             := scalafixSemanticdb.revision

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
ThisBuild / dynverSeparator                                := "-"

lazy val kafkaVersion = "2.4.1"

val kafkaDeps = Seq(
  "org.apache.kafka"  % "kafka-clients",
  "org.apache.kafka" %% "kafka"
).map(_ % kafkaVersion)

val dependencies = Seq(
  "com.github.scopt"           %% "scopt"                      % "3.7.1",
  "org.zalando"                %% "grafter"                    % "2.6.1",
  "com.typesafe.scala-logging" %% "scala-logging"              % "3.5.0",
  "io.circe"                   %% "circe-yaml"                 % "0.12.0",
  "io.circe"                   %% "circe-generic"              % "0.12.3",
  "org.typelevel"              %% "cats-core"                  % "1.5.0",
  "org.typelevel"              %% "cats-kernel"                % "1.5.0",
  "org.slf4j"                   % "log4j-over-slf4j"           % "1.7.25",
  "org.slf4j"                   % "slf4j-api"                  % "1.7.25",
  "ch.qos.logback"              % "logback-classic"            % "1.2.3"            % Runtime,
  "org.scalatest"              %% "scalatest"                  % "3.2.10"           % Test,
  "org.scalatestplus"          %% "mockito-3-12"               % "3.2.10.0"         % Test,
  "com.pirum"                  %% "scala-kafka-client-testkit" % s"$kafkaVersion-2" % Test,
  "org.mockito"                 % "mockito-all"                % "1.10.19"          % Test
) ++ kafkaDeps

val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin, JavaAppPackaging, UniversalDeployPlugin, DockerPlugin, AshScriptPlugin)
  .settings(
    defineCommandAliases,
    name                   := "kafka-configurator",
    organization           := "uk.sky",
    scalaVersion           := "2.12.10",
    sonatypeCredentialHost := "s01.oss.sonatype.org",
    sonatypeRepository     := "https://s01.oss.sonatype.org/service/local",
    homepage               := Some(url(scmUrl)),
    licenses               := List("BSD New" -> url("https://opensource.org/licenses/BSD-3-Clause")),
    developers             := List(
      Developer(
        "Sky UK OSS",
        "Sky UK OSS",
        sys.env.getOrElse("SONATYPE_EMAIL", scmUrl),
        url(scmUrl)
      )
    ),
    libraryDependencies ++= dependencies,
    scalacOptions += "-language:implicitConversions",
    scalacOptions -= "-Ywarn-value-discard",
    run / fork             := true,
    buildInfoSettings,
    dockerSettings
  )
