package com.sky.kafka.configurator

import common.KafkaIntSpec
import kafka.admin.AdminUtils
import org.scalatest.concurrent.Eventually

import scala.util.Success

class KafkaConfiguratorIntSpec extends KafkaIntSpec with Eventually {

  "KafkaConfigurator" should "create new topics in Kafka from a file" in {
    val topics = List("topic1", "topic2")

    topics.map(AdminUtils.topicExists(zkUtils, _) shouldBe false)

    Main.run(testArgs("/topic-configuration.yml")) shouldBe a[Success[_]]

    eventually {
      withClue("Topic exists: ") {
        topics.map(AdminUtils.topicExists(zkUtils, _) shouldBe true)
      }
    }
  }

  it should "still configure all topics when one fails" in {
    val correctTopics = List("correctConfig1", "correctConfig2")
    val errorTopic = "errorConfig"

    (correctTopics :+ errorTopic).map(AdminUtils.topicExists(zkUtils, _) shouldBe false)

    Main.run(testArgs("/topic-configuration-with-error.yml")) shouldBe a[Success[_]]

    eventually {
      withClue("Topic exists: ") {
        correctTopics.map(AdminUtils.topicExists(zkUtils, _) shouldBe true)
      }
      withClue("Topic doesn't exist: ") {
        AdminUtils.topicExists(zkUtils, errorTopic) shouldBe false
      }
    }
  }

  private def testArgs(filePath: String): Array[String] =
    Array(
      "-f", getClass.getResource(filePath).getPath,
      "--zookeeper", s"localhost:${kafkaServer.zookeeperPort.toString}"
    )
}
