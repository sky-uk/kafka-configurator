package com.sky.kafka.configurator

import java.io.{File, FileReader}

import cats.data.Reader
import cats.implicits._
import com.sky.kafka.configurator.error.ConfiguratorFailure

import scala.util.{Failure, Success, Try}

case class KafkaConfiguratorApp(configurator: TopicConfigurator) {

  def configureTopicsFrom(files: List[File]): Try[(List[ConfiguratorFailure], List[String])] =
    files.traverse { file =>
      for {
        fileReader <- Try(new FileReader(file))
        topics <- TopicConfigurationParser(fileReader).toTry
      } yield configureAll(topics)
    }.map(_.separate.bimap(_.flatten, _.flatten))

  private def configureAll(topics: List[Topic]): (List[ConfiguratorFailure], List[String]) = {
    val (errors, allLogs) = topics.map { topic =>
      configurator.configure(topic).run match {
        case Success((logs, _)) => Right(logs)
        case Failure(t) => Left(ConfiguratorFailure(topic.name, t))
      }
    }.separate
    (errors, allLogs.flatten)
  }
}

object KafkaConfiguratorApp {
  def reader: Reader[AppConfig, KafkaConfiguratorApp] =
    TopicConfigurator.reader.map(KafkaConfiguratorApp.apply)
}
