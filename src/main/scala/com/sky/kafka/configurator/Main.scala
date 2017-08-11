package com.sky.kafka.configurator

import java.io.{File, FileReader}

import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import org.zalando.grafter.{Rewriter, StopError, StopFailure, StopOk}
import scopt.OptionParser
import com.sky.BuildInfo

import scala.util.{Failure, Success, Try}

object Main extends LazyLogging {

  case object InvalidArgsException extends Exception

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

  def main(args: Array[String]): Unit = run(args) match {
    case Success(_) =>
      System.exit(0)
    case Failure(_) =>
      System.exit(1)
  }

  def run(args: Array[String]): Try[Unit] = parse(args) flatMap { conf =>
    logger.info(s"Running ${BuildInfo.name} ${BuildInfo.version}")
    val configurator = TopicConfigurator.reader(conf)
    val result: Try[Unit] = for {
      topics <- TopicConfigurationParser(new FileReader(conf.file))
      _ <- topics.map(configurator.configure).sequenceU
    } yield ()
    stopApp(configurator)
    result
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
