package com.sky.kafka.configurator

import cats.data.{Reader, WriterT}
import cats.implicits._
import com.sky.kafka.configurator.error.{ReplicationChangeFound, TopicNotFound}
import com.typesafe.scalalogging.LazyLogging

import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

case class TopicConfigurator(topicReader: TopicReader, topicWriter: TopicWriter) extends LazyLogging {

  type Logger[A] = WriterT[Try, List[String], A]

  def configure(topic: Topic): Logger[Unit] = {
    topicReader.fetch(topic.name) match {
      case Success(currentTopic) =>
        for {
          _ <- failIfDifferentReplicationFactor(currentTopic, topic)
          _ <- updatePartitionsIfDifferent(currentTopic, topic)
          _ <- updateConfigIfDifferent(currentTopic, topic)
        } yield ()
      case Failure(TopicNotFound(_)) =>
        topicWriter.create(topic)
          .withLog(s"Topic ${topic.name} not found: creating.")
      case Failure(NonFatal(t)) =>
        Failure(t).asWriter
    }
  }

  private def failIfDifferentReplicationFactor(oldTopic: Topic, newTopic: Topic): Logger[Unit] =
    if (oldTopic.replicationFactor != newTopic.replicationFactor)
      Failure(ReplicationChangeFound).asWriter
    else
      Success(())
        .withLog(s"Replication factor unchanged for ${newTopic.name}.")

  private def updatePartitionsIfDifferent(oldTopic: Topic, newTopic: Topic): Logger[Unit] =
    if (oldTopic.partitions != newTopic.partitions)
      topicWriter
        .updatePartitions(newTopic.name, newTopic.partitions)
        .withLog(s"Topic ${newTopic.name} has different number of partitions: updating.")
    else
      Success(())
        .withLog(s"No change in number of partitions for ${newTopic.name}")


  private def updateConfigIfDifferent(oldTopic: Topic, newTopic: Topic): Logger[Unit] =
    if (oldTopic.config != newTopic.config)
      topicWriter.updateConfig(newTopic.name, newTopic.config)
        .withLog(s"Topic ${newTopic.name} has different configuration: updating.")
    else
      Success(())
        .withLog(s"No change in config for ${newTopic.name}")

}

object TopicConfigurator {
  def reader: Reader[AppConfig, TopicConfigurator] = KafkaAdminClient.reader
    .map(kafkaAdminClient => TopicConfigurator(kafkaAdminClient, kafkaAdminClient))
}
