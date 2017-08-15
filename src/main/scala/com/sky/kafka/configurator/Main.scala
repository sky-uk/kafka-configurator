package com.sky.kafka.configurator

import java.io.{File, FileReader}

import cats.implicits._
import com.sky.BuildInfo
import com.sky.kafka.configurator.error.InvalidArgsException
import com.typesafe.scalalogging.LazyLogging
import org.zalando.grafter.{Rewriter, StopError, StopFailure, StopOk}
import scopt.OptionParser

import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

object Main extends LazyLogging {

  val parser = new OptionParser[AppConfig]("kafka-configurator") {
    opt[File]('f', "file").required().valueName("<file>")
      .action((x, c) => c.copy(file = x))
      .text("Topic configuration file")

    opt[String]("zookeeper").required()
      .action((x, c) => c.copy(zk = c.zk.copy(urls = x)))
      .text("Zookeeper URLs (comma-separated)")

    opt[Int]("zookeeper-timeout")
      .action((x, c) => c.copy(zk = c.zk.copy(timeout = x)))
      .text("Session and connection timeout for Zookeeper")
  }

  def main(args: Array[String]): Unit = {
    logger.info(s"Running ${BuildInfo.name} ${BuildInfo.version}")
    run(args) match {
      case Success(_) =>
        System.exit(0)
      case Failure(_) =>
        System.exit(1)
    }
  }

  def run(args: Array[String]): Try[Unit] =
    parse(args).flatMap { conf =>
      val configurator = TopicConfigurator.reader(conf)
      val result = configureTopicsFromFile(conf.file, configurator)

      stopApp(configurator)
      result
    }

  private def configureTopicsFromFile(topicConfigYml: File, configurator: TopicConfigurator) =
    for {
      topics <- TopicConfigurationParser(new FileReader(topicConfigYml)).toTry
      _ <- topics.traverseU { topic =>
        configurator.configure(topic).run.map {
          case (logs, _) =>
            logs.foreach(log => logger.info(log))
        }.recover {
          case NonFatal(throwable) =>
            logger.error(s"Failed to configure ${topic.name}", throwable)
        }
      }
    } yield ()

  def parse(args: Seq[String]): Try[AppConfig] =
    parser.parse(args, AppConfig()) match {
      case Some(config) => Success(config)
      case None => Failure(InvalidArgsException)
    }

  def stopApp(configurator: TopicConfigurator) {
    Rewriter.stop(configurator).value.foreach {
      case StopOk(msg) => logger.debug(s"Component stopped: $msg")
      case StopError(msg, ex) => logger.warn(s"Error whilst stopping component: $msg", ex)
      case StopFailure(msg) => logger.warn(s"Failure whilst stopping component: $msg")
    }
  }
}
