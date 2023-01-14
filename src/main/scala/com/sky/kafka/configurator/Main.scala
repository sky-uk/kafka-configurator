package com.sky.kafka.configurator

import com.sky.BuildInfo
import com.sky.kafka.configurator.config.ConfigParsing
import com.sky.kafka.configurator.error.ConfiguratorFailure
import com.typesafe.scalalogging.LazyLogging
import org.zalando.grafter._

import scala.util.{Failure, Success, Try}

object Main extends LazyLogging {

  def main(args: Array[String]): Unit = {
    logger.info(s"Running ${BuildInfo.name} ${BuildInfo.version} with args: ${args.mkString(", ")}")

    run(args, sys.env) match {
      case Success((errors, infoLogs)) =>
        errors.foreach(e => logger.warn(s"${e.getMessage}. Cause: ${e.getCause.getMessage}"))
        infoLogs.foreach(msg => logger.info(msg))
        if (errors.isEmpty) System.exit(0) else System.exit(1)
      case Failure(t)                  =>
        logger.error(t.getMessage)
        System.exit(1)
    }
  }

  def run(args: Array[String], envVars: Map[String, String]): Try[(List[ConfiguratorFailure], List[String])] =
    ConfigParsing.parse(args, envVars).flatMap { conf =>
      val app    = KafkaConfiguratorApp.reader(conf)
      val result = app.configureTopicsFrom(conf.files.toList)
      stop(app)
      result
    }

  private def stop(app: KafkaConfiguratorApp): Unit =
    Rewriter.stopAll(app).value.foreach {
      case StopOk(msg)        => logger.debug(s"Component stopped: $msg")
      case StopError(msg, ex) => logger.warn(s"Error whilst stopping component: $msg", ex)
      case StopFailure(msg)   => logger.warn(s"Failure whilst stopping component: $msg")
    }
}
