package com.sky.kafka.configurator

import common.BaseSpec

class KafkaConfiguratorAppSpec extends BaseSpec {


  "configureTopics" should "success when all topics have been configured successfully" in {
    val topicConfigurator: TopicConfigurator = ???
    val kafkaConfiguratorApp = KafkaConfiguratorApp(topicConfigurator)
    val topics = List[Topic]
    val result = kafkaConfiguratorApp.configureTopics(topics)
  }

}
