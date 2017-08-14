package com.sky.kafka.configurator

import java.io.{File, FileReader}

import cats.implicits._
import com.sky.BuildInfo
import com.sky.kafka.configurator.error.InvalidArgsException
import com.typesafe.scalalogging.LazyLogging
import org.zalando.grafter.{Rewriter, StopError, StopFailure, StopOk}
import scopt.OptionParser

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
      case Success((logs, _)) =>
        logs.foreach(log => logger.info(log))
        System.exit(0)
      case Failure(t) =>
        logger.error(s"Failed to configure topics", t)
        System.exit(1)
    }
  }

  def run(args: Array[String]): Try[(List[String], Unit)] =
    parse(args).flatMap { conf =>
      val configurator = TopicConfigurator.reader(conf)
      val result = for {
        topics <- TopicConfigurationParser(new FileReader(conf.file))
          .toTry.withLog("Successfully parsed topic configuration file.")
        _ <- topics.traverse(configurator.configure)
      } yield ()
      stopApp(configurator)
      result.run
    }

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
