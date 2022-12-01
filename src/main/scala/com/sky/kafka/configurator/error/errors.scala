package com.sky.kafka.configurator.error

case object InvalidArgsException extends Exception

case class ConfiguratorFailure(topicName: String, throwable: Throwable)
  extends Exception(s"Failed to configure $topicName", throwable)

case class TopicNotFound(topicName: String) extends Exception(s"$topicName not found")

case object ReplicationChangeFound extends Exception("Changing replication factor is unsupported")
