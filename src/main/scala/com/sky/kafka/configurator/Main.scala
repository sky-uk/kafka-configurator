package com.sky.kafka.configurator

import java.io.File

import com.sky.BuildInfo
import com.sky.kafka.configurator.error.{ConfiguratorFailure, InvalidArgsException}
import com.typesafe.scalalogging.LazyLogging
import org.zalando.grafter._
import scopt.OptionParser

import scala.util.{Failure, Success, Try}

object Main extends LazyLogging {

  val parser = new OptionParser[AppConfig]("kafka-configurator") {
    opt[File]('f', "file").required().valueName("<file>")
      .action((x, c) => c.copy(file = x))
      .text("Topic configuration file")
      .validate(file => if (file.exists()) success else failure(s"$file does not exist."))

    opt[String]("zookeeper").required()
      .action((x, c) => c.copy(zk = c.zk.copy(urls = x)))
      .text("Zookeeper URLs (comma-separated)")

    opt[Int]("zookeeper-timeout")
      .action((x, c) => c.copy(zk = c.zk.copy(timeout = x)))
      .text("Session and connection timeout for Zookeeper")
  }

  def main(args: Array[String]) {
    logger.info(s"Running ${BuildInfo.name} ${BuildInfo.version} with args: ${args.mkString(", ")}")
    run(args) match {
      case Success((errors, infoLogs)) =>
        errors.foreach(e => logger.warn(s"${e.getMessage}. Cause: ${e.getCause.getMessage}"))
        infoLogs.foreach(msg => logger.info(msg))
        if (errors.isEmpty) System.exit(0) else System.exit(1)
      case Failure(t) =>
        logger.warn(t.getMessage)
        System.exit(1)
    }
  }

  def run(args: Array[String]): Try[(List[ConfiguratorFailure], List[String])] =
    parse(args).flatMap { conf =>
      val app = KafkaConfiguratorApp.reader(conf)
      val result = app.configureTopicsFrom(conf.file)
      stop(app)
      result
    }

  def parse(args: Seq[String]): Try[AppConfig] =
    parser.parse(args, AppConfig()) match {
      case Some(config) => Success(config)
      case None => Failure(InvalidArgsException)
    }

  def stop(app: KafkaConfiguratorApp) {
    Rewriter.stop(app).value.foreach {
      case StopOk(msg) => logger.debug(s"Component stopped: $msg")
      case StopError(msg, ex) => logger.warn(s"Error whilst stopping component: $msg", ex)
      case StopFailure(msg) => logger.warn(s"Failure whilst stopping component: $msg")
    }
  }
}
