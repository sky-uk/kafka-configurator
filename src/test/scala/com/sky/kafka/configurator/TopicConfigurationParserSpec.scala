package com.sky.kafka.configurator

import java.io.StringReader

import common.BaseSpec
import io.circe.DecodingFailure

class TopicConfigurationParserSpec extends BaseSpec {

  "parseAndConfigure" should "extract topics from yml and configure them" in {
    val yml =
      """
        |topic1:
        |  partitions: 10
        |  replication: 3
        |  config:
        |    cleanup.policy: compact
        |    delete.retention.ms: 86400000
        |    min.compaction.lag.ms: 21600000
        |    retention.ms: 0
        |    min.insync.replicas: 2
        |    min.cleanable.dirty.ratio: 0.1
        |
        |topic2:
        |  partitions: 5
        |  replication: 2
        |  config:
        |    cleanup.policy: delete
        |    delete.retention.ms: 0
        |    retention.ms: 604800000
        |    min.insync.replicas: 2
        |    retention.ms: 2592000000
      """.stripMargin

    val topics = List(
      Topic("topic1", 10, 3, Map(
        "cleanup.policy" -> "compact",
        "delete.retention.ms" -> "86400000",
        "min.compaction.lag.ms" -> "21600000",
        "retention.ms" -> "0",
        "min.insync.replicas" -> "2",
        "min.cleanable.dirty.ratio" -> "0.1"
      )),
      Topic("topic2", 5, 2, Map(
        "cleanup.policy" -> "delete",
        "delete.retention.ms" -> "0",
        "retention.ms" -> "604800000",
        "min.insync.replicas" -> "2",
        "retention.ms" -> "2592000000"
      ))
    )

    TopicConfigurationParser(new StringReader(yml)).right.get shouldBe topics
  }

  it should "fail if any of the topics have invalid configuration" in {
    val yml =
      """
        |topic1:
        |  this.is.not.correct: 42
        |
        |topic2:
        |  partitions: 5
        |  replication: 2
        |  config:
        |    cleanup.policy: delete
        |    delete.retention.ms: 0
        |    retention.ms: 86400000
        |    min.insync.replicas: 2
      """.stripMargin

    TopicConfigurationParser(new StringReader(yml)).left.get shouldBe a[DecodingFailure]
  }

  it should "fail if any of the config values are not a string or number" in {
    val yml =
      """
        |topic1:
        |  partitions: 5
        |  replication: 2
        |  config:
        |    cleanup.policy: delete
        |    delete.retention.ms: [0, 0]
        |    retention.ms: 86400000
        |    min.insync.replicas: 2
      """.stripMargin

    TopicConfigurationParser(new StringReader(yml)).left.get shouldBe a[DecodingFailure]
  }

  it should "parse topics in the same order as they appear in the YML" in {
    val topics = (1 to 100).toList.map(i => s"topic$i")

    val yml = topics.map { topic =>
      s"""
        |$topic:
        |  partitions: 10
        |  replication: 3
        |  config:
        |    cleanup.policy: delete
      """.stripMargin
    }.mkString("\n")

    TopicConfigurationParser(new StringReader(yml)).right.get.map(_.name) shouldBe topics
  }

}
