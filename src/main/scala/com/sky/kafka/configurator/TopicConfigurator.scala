package com.sky.kafka.configurator

import cats.data.Reader
import cats.implicits._
import com.sky.kafka.configurator.error.{ReplicationChangeFound, TopicNotFound}
import com.typesafe.scalalogging.LazyLogging

import scala.util.control.NonFatal
import scala.util.{Failure, Success}

case class TopicConfigurator(topicReader: TopicReader, topicWriter: TopicWriter) extends LazyLogging {

  def configure(topic: Topic): Logger[Unit] =
    topicReader.fetch(topic.name) match {
      case Success(currentTopic) =>
        updateTopic(currentTopic, topic)
      case Failure(TopicNotFound(_)) =>
        topicWriter.create(topic)
          .withLog(s"Topic ${topic.name} not found: creating.")
      case Failure(NonFatal(t)) =>
        Failure(t).asWriter
    }

  private def updateTopic(oldTopic: Topic, newTopic: Topic): Logger[Unit] = {

    def ifDifferent[T](oldValue: T, newValue: T)
                      (updateOperation: (Topic, Topic) => Logger[Unit])
                      (messageIfSame: String): Logger[Unit] =
      if (oldValue != newValue)
        updateOperation(oldTopic, newTopic)
      else
        Success(()).withLog(messageIfSame)

    for {
      _ <- ifDifferent(oldTopic.replicationFactor, newTopic.replicationFactor)(failReplicationChange)(s"Replication factor unchanged for ${newTopic.name}.")
      _ <- ifDifferent(oldTopic.partitions, newTopic.partitions)(updatePartitions)(s"No change in number of partitions for ${newTopic.name}")
      _ <- ifDifferent(oldTopic.config, newTopic.config)(updateConfig)(s"No change in config for ${newTopic.name}")
    } yield ()
  }

  private def failReplicationChange(oldTopic: Topic, newTopic: Topic): Logger[Unit] =
    Failure(ReplicationChangeFound).asWriter

  private def updatePartitions(oldTopic: Topic, newTopic: Topic): Logger[Unit] =
    topicWriter
      .updatePartitions(newTopic.name, newTopic.partitions)
      .withLog(s"Topic ${newTopic.name} has different number of partitions: updating.")

  private def updateConfig(oldTopic: Topic, newTopic: Topic): Logger[Unit] =
    topicWriter
      .updateConfig(newTopic.name, newTopic.config)
      .withLog(s"Topic ${newTopic.name} has different configuration: updating.")
}

object TopicConfigurator {
  def reader: Reader[AppConfig, TopicConfigurator] = KafkaAdminClient.reader
    .map(kafkaAdminClient => TopicConfigurator(kafkaAdminClient, kafkaAdminClient))
}
