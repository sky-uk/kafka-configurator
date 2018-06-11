package com.sky.kafka.configurator

import java.util.UUID

import com.sky.kafka.configurator.error.TopicNotFound
import common.KafkaIntSpec
import org.scalatest.concurrent.Eventually
import org.zalando.grafter.StopOk

import scala.util.Success

class KafkaTopicAdminSpec extends KafkaIntSpec with Eventually {

  lazy val adminClient = KafkaTopicAdmin(kafkaAdminClient)

  def someTopic = Topic(UUID.randomUUID().toString, partitions = 3, replicationFactor = 1, defaultTopicProperties)

  "create" should "create topic using the given configuration" in {
    val inputTopic = someTopic.copy(config = defaultTopicProperties + (
      "retention.ms" -> "50000"
    ))

    adminClient.create(inputTopic) shouldBe Success(())

    eventually {
      val createdTopic = adminClient.fetch(inputTopic.name)
      createdTopic shouldBe Success(inputTopic)
    }
  }

  "fetch" should "return a failure when fetching a topic that does not exist" in {
    adminClient.fetch("non-existent-topic").failure.exception shouldBe TopicNotFound("non-existent-topic")
  }

  "updateConfig" should "update an existing topics configuration" in {
    val inputTopic = someTopic.copy(config = defaultTopicProperties + (
      "retention.ms" -> "5000"
    ))
    adminClient.create(inputTopic) shouldBe Success(())

    val updatedTopic = inputTopic.copy(config = defaultTopicProperties + (
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

  "stop" should "return StopOk when zkUtils has been closed successfully" in {
    val adminClient = KafkaTopicAdmin(kafkaAdminClient)
    adminClient.stop.value shouldBe StopOk("KafkaAdminClient")

    adminClient.fetch("test").failure.exception should have {
      'message ("org.apache.kafka.common.errors.TimeoutException: The AdminClient thread is not accepting new calls.")
    }

  }
}
