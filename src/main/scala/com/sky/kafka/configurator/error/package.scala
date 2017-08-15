package com.sky.kafka.configurator

package object error {

  case object InvalidArgsException extends Exception

  case class TopicNotFound(topicName: String) extends Exception(s"$topicName not found")

  case object ReplicationChangeFound extends Exception("Changing replication factor is unsupported")

}
