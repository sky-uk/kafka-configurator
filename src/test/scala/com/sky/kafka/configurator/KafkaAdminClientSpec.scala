package com.sky.kafka.configurator

import java.util.UUID

import common.KafkaIntSpec
import org.scalatest.concurrent.Eventually
import org.zalando.grafter.StopOk

import scala.util.{Failure, Success}

class KafkaAdminClientSpec extends KafkaIntSpec with Eventually {

  lazy val adminClient = KafkaAdminClient(zkUtils)

  def someTopic = Topic(UUID.randomUUID().toString, partitions = 3, replicationFactor = 1, Map.empty)

  "create" should "create topic using the given configuration" in {
    val inputTopic = someTopic.copy(config = Map(
      "retention.ms" -> "50000"
    ))

    adminClient.create(inputTopic) shouldBe Success(())

    eventually {
      val createdTopic = adminClient.fetch(inputTopic.name)
      createdTopic shouldBe Success(inputTopic)
    }
  }

  it should "return a failure if given an unrecognised config entry" in {
    val inputTopic = someTopic.copy(config = Map(
      "invalid.key" -> "invalid.value"
    ))

    adminClient.create(inputTopic) shouldBe a[Failure[_]]
  }

  it should "return a failure if replication factor is greater than number of available brokers" in {
    val inputTopic = someTopic.copy(replicationFactor = 2)

    adminClient.create(inputTopic) shouldBe a[Failure[_]]
  }

  "fetch" should "return a failure when fetching a topic that does not exist" in {
    adminClient.fetch("non-existent-topic").failure.exception shouldBe TopicNotFound("non-existent-topic")
  }

  "updateConfig" should "update an existing topics configuration" in {
    val inputTopic = someTopic.copy(config = Map(
      "retention.ms" -> "5000"
    ))
    adminClient.create(inputTopic) shouldBe Success(())

    val updatedTopic = inputTopic.copy(config = Map(
      "retention.ms" -> "10000"
    ))
    adminClient.updateConfig(updatedTopic.name, updatedTopic.config) shouldBe Success(())

    eventually {
      adminClient.fetch(inputTopic.name) shouldBe Success(updatedTopic)
    }
  }

  "updatePartitions" should "set the number of partitions to the given value" in {
    val topic = someTopic.copy(partitions = 2)
    adminClient.create(topic) shouldBe Success(())

    adminClient.updatePartitions(topic.name, 5) shouldBe Success(())

    eventually {
      adminClient.fetch(topic.name).get.partitions shouldBe 5
    }
  }

  it should "return a failure when updating a topic that doesn't exist" in {
    adminClient.updatePartitions("non-existent-topic", 5) shouldBe a[Failure[_]]
  }

  it should "return a failure if decreasing the number of partitions" in {
    val topic = someTopic.copy(partitions = 3)
    adminClient.create(topic) shouldBe Success(())

    adminClient.updatePartitions(topic.name, 2) shouldBe a[Failure[_]]
  }

  "stop" should "return StopOk when zkUtils has been closed successfully" in {
    val adminClient = KafkaAdminClient(zkUtils)
    adminClient.stop.value shouldBe StopOk("zkUtils")
    adminClient.fetch("test").failure.exception shouldBe a[IllegalStateException]
  }
}
