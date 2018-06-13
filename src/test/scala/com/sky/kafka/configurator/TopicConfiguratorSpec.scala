package com.sky.kafka.configurator

import com.sky.kafka.configurator.error.{ReplicationChangeFound, TopicNotFound}
import common.BaseSpec
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar

import scala.util.{Failure, Success}

class TopicConfiguratorSpec extends BaseSpec with MockitoSugar {

  "TopicConfigurator" should "create a topic if it doesn't exist" in new TestContext {
    when(topicReader.fetch(topic.name)).thenReturn(Failure(TopicNotFound(topic.name)))
    when(topicWriter.create(topic)).thenReturn(Success())

    configurator.configure(topic)

    verify(topicWriter).create(topic)
  }

  it should "fail if a topic doesn't exist and topic creation fails" in new TestContext {
    when(topicReader.fetch(topic.name)).thenReturn(Failure(TopicNotFound(topic.name)))
    when(topicWriter.create(topic)).thenReturn(Failure(someError))

    configurator.configure(topic).run.failure.exception shouldBe a[Exception]

    verify(topicWriter).create(topic)
  }

  it should "propagate errors retrieving current state" in new TestContext {
    when(topicReader.fetch(topic.name)).thenReturn(Failure(someError))

    configurator.configure(topic).run.failure.exception shouldBe a[Exception]

    verifyZeroInteractions(topicWriter)
  }

  it should "update configuration if it has changed" in new TestContext {
    when(topicReader.fetch(topic.name)).thenReturn(Success(topic.copy(config = Map("a" -> "A"))))

    configurator.configure(topic.copy(config = Map("b" -> "B")))

    verify(topicWriter).updateConfig(topic.name, Map("b" -> "B"))
  }

  it should "update number of partitions if it has changed" in new TestContext {
    when(topicReader.fetch(topic.name)).thenReturn(Success(topic.copy(partitions = 3)))

    configurator.configure(topic.copy(partitions = 5))

    verify(topicWriter).updatePartitions(topic.name, 5)
  }

  it should "do nothing if nothing has changed" in new TestContext {
    val topicName = topic.name
    val fetched = topic.copy(config = Map(
      "some.default.key" -> "some.default.value",
      "some.config.key" -> "some.config.value"
    ))

    val configured = topic.copy(config = Map(
      "some.config.key" -> "some.config.value"
    ))

    when(topicReader.fetch(topicName)).thenReturn(Success(fetched))

    configurator.configure(configured)

    verifyZeroInteractions(topicWriter)
  }

  it should "not change config if it fails changing partitions" in new TestContext {
    val oldTopic = topic.copy(partitions = 4, config = Map("a" -> "A"))
    val newTopic = topic.copy(partitions = 5, config = Map("b" -> "B"))
    when(topicReader.fetch(newTopic.name)).thenReturn(Success(oldTopic))
    when(topicWriter.updatePartitions(newTopic.name, newTopic.partitions)).thenReturn(Failure(someError))
    when(topicWriter.updateConfig(newTopic.name, newTopic.config)).thenReturn(Success(()))

    configurator.configure(newTopic).run.failure.exception shouldBe someError

    verify(topicWriter, times(1)).updatePartitions(newTopic.name, newTopic.partitions)
    verify(topicWriter, times(0)).updateConfig(newTopic.name, newTopic.config)
  }

  it should "change partitions even if it fails changing config" in new TestContext {
    val oldTopic = topic.copy(partitions = 4, config = Map("a" -> "A"))
    val newTopic = topic.copy(partitions = 5, config = Map("b" -> "B"))
    when(topicReader.fetch(newTopic.name)).thenReturn(Success(oldTopic))
    when(topicWriter.updatePartitions(newTopic.name, newTopic.partitions)).thenReturn(Success(()))
    when(topicWriter.updateConfig(newTopic.name, newTopic.config)).thenReturn(Failure(someError))

    configurator.configure(newTopic).run.failure.exception shouldBe someError

    verify(topicWriter, times(1)).updatePartitions(newTopic.name, newTopic.partitions)
    verify(topicWriter, times(1)).updateConfig(newTopic.name, newTopic.config)
  }

  it should "fail if the topic has a different replication factor" in new TestContext {
    val oldTopic = topic.copy(replicationFactor = 5)
    val newTopic = oldTopic.copy(replicationFactor = 6)
    when(topicReader.fetch(newTopic.name)).thenReturn(Success(oldTopic))

    configurator.configure(newTopic).run.failure.exception shouldBe ReplicationChangeFound

    verifyZeroInteractions(topicWriter)
  }

  trait TestContext {
    val topicReader: TopicReader = mock[TopicReader]
    val topicWriter: TopicWriter = mock[TopicWriter]
    val configurator = TopicConfigurator(topicReader, topicWriter)

    val topic = Topic("test-topic", 1, 1, Map.empty)
    val someError = new Exception("Some unexpected error")
  }

}
