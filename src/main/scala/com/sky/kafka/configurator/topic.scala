package com.sky.kafka.configurator

import scala.util.Try

case class Topic(name: String, partitions: Int, replicationFactor: Int, config: Map[String, String])

trait TopicReader {
  def fetch(topicName: String): Try[Topic]
}

trait TopicWriter {
  def create(topic: Topic): Try[Unit]

  def updateConfig(topicName: String, config: Map[String, Object]): Try[Unit]

  def updatePartitions(topicName: String, numPartitions: Int): Try[Unit]
}
