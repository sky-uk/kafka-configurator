package com.sky.kafka.configurator

import cats.data.Reader
import com.typesafe.scalalogging.LazyLogging

import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

case class TopicConfigurator(topicReader: TopicReader, topicWriter: TopicWriter) extends LazyLogging {

  def configure(topic: Topic): Try[Unit] = {
    tryConfigure(topic).recoverWith {
      case NonFatal(t) =>
        logger.error(s"Error occurred whilst configuring topic ${topic.name}: ${t.getMessage}")
        Failure(t)
    }
  }

  private def tryConfigure(topic: Topic): Try[Unit] = {
    for {
      currentTopic <- topicReader.fetch(topic.name)
      _ <- failIfDifferentReplicationFactor(currentTopic, topic)
      _ <- updatePartitionsIfDifferent(currentTopic, topic)
      _ <- updateConfigIfDifferent(currentTopic, topic)
    } yield ()
  }.recoverWith {
    case TopicNotFound(_) =>
      logger.info(s"Topic ${topic.name} not found: creating.")
      topicWriter.create(topic)
  }

  private def failIfDifferentReplicationFactor(oldTopic: Topic, newTopic: Topic): Try[Unit] =
    if (oldTopic.replicationFactor != newTopic.replicationFactor)
      Failure(ReplicationChangeFound)
    else
      Success(())


  private def updatePartitionsIfDifferent(oldTopic: Topic, newTopic: Topic): Try[Unit] =
    if (oldTopic.partitions != newTopic.partitions) {
      logger.info(s"Topic ${newTopic.name} has different number of partitions: updating.")
      topicWriter.updatePartitions(newTopic.name, newTopic.partitions)
    } else {
      Success(())
    }

  private def updateConfigIfDifferent(oldTopic: Topic, newTopic: Topic): Try[Unit] =
    if (oldTopic.config != newTopic.config) {
      logger.info(s"Topic ${newTopic.name} has different configuration: updating.")
      topicWriter.updateConfig(newTopic.name, newTopic.config)
    } else {
      Success(())
    }
}

object TopicConfigurator {
  def reader: Reader[AppConfig, TopicConfigurator] = KafkaAdminClient.reader.map(c => TopicConfigurator(c, c))
}
