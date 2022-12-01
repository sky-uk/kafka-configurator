package com.sky.kafka.configurator

import java.util.UUID

import com.sky.kafka.configurator.config.Config
import com.sky.kafka.configurator.error.TopicNotFound
import com.sky.kafka.matchers.TopicMatchers
import common.KafkaIntSpec
import org.scalatest.concurrent.Eventually
import org.zalando.grafter.StopOk

import scala.util.{Failure, Success}

class KafkaTopicAdminSpec extends KafkaIntSpec with Eventually with TopicMatchers {

  def someTopic: Topic =
    Topic(UUID.randomUUID().toString, partitions = 3, replicationFactor = 1, Map.empty[String, String])

  "create" should "create topic using the given configuration" in new TestContext {
    val inputTopic = someTopic.copy(config = Map("retention.ms" -> "50000"))

    adminClient.create(inputTopic) shouldBe Success(())

    eventually {
      val createdTopic = adminClient.fetch(inputTopic.name)
      createdTopic.success.value should beEquivalentTo(inputTopic)
    }
  }

  it should "return a failure if given an unrecognised config entry" in new TestContext {
    val inputTopic = someTopic.copy(config = Map("invalid.key" -> "invalid.value"))

    adminClient.create(inputTopic) shouldBe a[Failure[_]]
  }

  it should "return a failure if replication factor is greater than number of available brokers" in new TestContext {
    val inputTopic = someTopic.copy(replicationFactor = 2)

    adminClient.create(inputTopic) shouldBe a[Failure[_]]
  }

  "fetch" should "return a failure when fetching a topic that does not exist" in new TestContext {
    adminClient.fetch("non-existent-topic").failure.exception shouldBe TopicNotFound("non-existent-topic")
  }

  "updateConfig" should "update an existing topics configuration" in new TestContext {
    val inputTopic = someTopic.copy(config =
      Map(
        "retention.ms" -> "5000"
      )
    )
    adminClient.create(inputTopic) shouldBe Success(())

    val updatedTopic = inputTopic.copy(config =
      Map(
        "retention.ms" -> "10000"
      )
    )
    adminClient.updateConfig(updatedTopic.name, updatedTopic.config) shouldBe Success(())

    eventually {
      adminClient.fetch(inputTopic.name).success.value should beEquivalentTo(updatedTopic)
    }
  }

  it should "fail to update with an invalid property" in new TestContext {
    val inputTopic = someTopic.copy(config =
      Map(
        "retention.ms" -> "5000"
      )
    )
    adminClient.create(inputTopic) shouldBe Success(())

    val updatedTopic = inputTopic.copy(config =
      Map(
        "invalid.key" -> "invalid.value"
      )
    )

    adminClient.updateConfig(updatedTopic.name, updatedTopic.config) shouldBe a[Failure[_]]
  }

  "updatePartitions" should "set the number of partitions to the given value" in new TestContext {
    val topic = someTopic.copy(partitions = 2)
    adminClient.create(topic) shouldBe Success(())

    adminClient.updatePartitions(topic.name, 5) shouldBe Success(())

    eventually {
      adminClient.fetch(topic.name).success.value.partitions shouldBe 5
    }
  }

  it should "return a failure when updating a topic that doesn't exist" in new TestContext {
    adminClient.updatePartitions("non-existent-topic", 5) shouldBe a[Failure[_]]
  }

  it should "return a failure if decreasing the number of partitions" in new TestContext {
    val topic = someTopic.copy(partitions = 3)
    adminClient.create(topic) shouldBe Success(())

    adminClient.updatePartitions(topic.name, 2) shouldBe a[Failure[_]]
  }

  "stop" should "return StopOk when zkUtils has been closed successfully" in new TestContext {
    override val adminClient = KafkaTopicAdmin(kafkaAdminClient)
    adminClient.stop.value shouldBe StopOk("KafkaAdminClient")

    adminClient.fetch("test").failure.exception should have {
      'message("org.apache.kafka.common.errors.TimeoutException: The AdminClient thread is not accepting new calls.")
    }

  }

  "reader" should "pass props from config to Kafka admin client" in new TestContext {
    val overrideBootstrapServers = Config().copy(props = Map("bootstrap.servers" -> s"localhost:$kafkaPort"))
    noException shouldBe thrownBy(KafkaTopicAdmin.reader(overrideBootstrapServers))
  }

  private trait TestContext {
    val adminClient = KafkaTopicAdmin(kafkaAdminClient)
  }
}
