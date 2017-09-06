package com.sky.kafka.configurator

import java.io.{File, FileReader}

import cats.data.Validated.{Invalid, Valid}
import cats.implicits._
import cats.data.{Reader, Validated, ValidatedNel}
import com.sky.kafka.configurator.Main.logger
import com.sky.kafka.configurator.error.TopicConfigException
import com.typesafe.scalalogging.LazyLogging

import scala.util.{Failure, Success, Try}
import scala.util.control.NonFatal

case class KafkaConfiguratorApp(configurator: TopicConfigurator) extends LazyLogging {

  implicit def eitherToValidated[A, B](either: Either[A, B]): ValidatedNel[A, B] = either.toValidatedNel

  def configureTopicsFrom(file: File): List[Validated[Exception, Unit]] =
    Either.catchNonFatal(new FileReader(file)) andThen TopicConfigurationParser.apply andThen configureTopics


  private def configureTopics(topics: List[Topic]): List[Try[Unit]] = {
    topics.map { topic =>
      configurator.configure(topic).run.transform({
        case ((logs, _)) =>
          Success(logs.foreach(log => logger.info(log)))
      }, {
        case NonFatal(throwable) =>
          Failure(TopicConfigException(topic.name, throwable))
      })
    }
//    val x: Try[(List[String], Unit)] = configurator.configure(topic).run
  }

}

object KafkaConfiguratorApp {
  def reader: Reader[AppConfig, KafkaConfiguratorApp] =
    TopicConfigurator.reader.map(KafkaConfiguratorApp.apply)
}
