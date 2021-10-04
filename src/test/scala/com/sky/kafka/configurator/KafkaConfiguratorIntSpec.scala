package com.sky.kafka.configurator

import common.KafkaIntSpec
import org.scalatest.concurrent.Eventually

import scala.util.Success

class KafkaConfiguratorIntSpec extends KafkaIntSpec with Eventually {

  "KafkaConfigurator" should "create new topics in Kafka from multiple input files" in {
    val topics = List("topic1", "topic2", "topic3")

    topics.map(topicExists(_) shouldBe false)

    Main.run(testArgs(Seq("/topic-configuration.yml", "/topic-configuration-2.yml")), Map.empty) shouldBe a[Success[_]]

    eventually {
      withClue("Topic exists: ") {
        topics.map(kafkaAdminClient.listTopics().names().get().contains(_) shouldBe true)
      }
    }
  }

  it should "still configure all topics when one fails" in {
    val correctTopics = List("correctConfig1", "correctConfig2")
    val errorTopic = "errorConfig"

    (correctTopics :+ errorTopic).map(topicExists(_) shouldBe false)

    Main.run(testArgs(Seq("/topic-configuration-with-error.yml")), Map.empty) shouldBe a[Success[_]]

    eventually {
      withClue("Topic exists: ") {
        correctTopics.map(topicExists(_) shouldBe true)
      }
      withClue("Topic doesn't exist: ") {
        topicExists(errorTopic) shouldBe false
      }
    }
  }

  it should "configure topics from correct files if another input file is empty" in {
    val topic = "topic4"

    topicExists(topic) shouldBe false

    Main.run(testArgs(Seq("/topic-configuration-3.yml", "/no-topics.yml")), Map.empty) shouldBe a[Success[_]]

    eventually {
      withClue("Topic exists: ") {
        topicExists(topic) shouldBe true
      }
    }
  }

  it should "configure topics defined using yaml anchors and aliases" in {
    val topics = List("topic-anchor", "topic-alias")

    topics.map(topicExists(_) shouldBe false)

    Main.run(testArgs(Seq("/topic-configuration-anchors.yml")), Map.empty) shouldBe a[Success[_]]

    eventually {
      withClue("Topic exists: ") {
        topics.map(topicExists(_) shouldBe true)
      }
    }
  }

  private def testArgs(filePaths: Seq[String]): Array[String] =
    Array(
      "-f", filePaths.map(path => getClass.getResource(path).getPath).mkString(","),
      "--bootstrap-servers", s"localhost:${kafkaServer.kafkaPort}"
    )

  private def topicExists(topic: String): Boolean = {
    kafkaAdminClient.listTopics().names().get().contains(topic)
  }
}
