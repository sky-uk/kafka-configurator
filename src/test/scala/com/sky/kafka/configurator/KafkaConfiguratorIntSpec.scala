package com.sky.kafka.configurator

import common.KafkaIntSpec
import kafka.admin.AdminUtils
import org.scalatest.concurrent.Eventually

import scala.util.Success

class KafkaConfiguratorIntSpec extends KafkaIntSpec with Eventually {

  "KafkaConfigurator" should "create new topics in Kafka from a file" in {
    val args = Array(
      "-f", getClass.getResource("/topic-configuration.yml").getPath,
      "--zookeeper", s"localhost:${kafkaServer.zookeeperPort.toString}"
    )
    val topics = List("topic1", "topic2")

    topics.map(AdminUtils.topicExists(zkUtils, _) shouldBe false)

    Main.run(args) shouldBe a[Success[_]]

    eventually {
      withClue("Topic exists: ") {
        topics.map(AdminUtils.topicExists(zkUtils, _) shouldBe true)
      }
    }
  }

}
