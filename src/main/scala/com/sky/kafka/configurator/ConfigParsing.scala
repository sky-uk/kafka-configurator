package com.sky.kafka.configurator

import java.io.File

import com.sky.kafka.configurator.error.InvalidArgsException
import org.apache.kafka.clients.admin.AdminClientConfig
import scopt.OptionParser

import scala.util.Try
import scala.collection.JavaConverters._

object ConfigParsing {

  private val parser = new OptionParser[AppConfig]("kafka-configurator") {
    opt[Seq[File]]('f', "files").required().valueName("<file1>,<file2>...")
      .action((x, c) => c.copy(files = x))
      .text("Topic configuration files")
      .validate(files => if (files.forall(_.exists)) success else failure(s"One of ${files.mkString(",")} does not exist."))

    opt[String]("bootstrap-servers").required()
      .action((x, c) => c.copy(bootstrapServers = x))
      .text("Kafka brokers URLs for bootstrap (comma-separated)")

    opt[Map[String, String]]("properties").optional()
      .action((x, c) => c.copy(props = x))
      .text("Kafka admin client config as comma-separated pairs")
  }

  def parse(args: Seq[String], envVars: Map[String, String]): Try[AppConfig] =
    parser.parse(args, AppConfig()).map { appConfig =>
      val providedConfig = appConfig.props ++ collectKafkaConfigFrom(envVars)
      appConfig.copy(props = providedConfig.filterKeys(AdminClientConfig.configNames().asScala.contains))
    }.toRight(InvalidArgsException).toTry

  private def collectKafkaConfigFrom(envVars: Map[String, String]): Map[String, String] =
    envVars.map {
      case (k, v) =>
        k.split("KAFKA_").tail.mkString.replaceAll("_", ".").toLowerCase -> v
    }
}
