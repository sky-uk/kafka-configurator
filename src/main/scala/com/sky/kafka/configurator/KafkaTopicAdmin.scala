package com.sky.kafka.configurator

import java.util.concurrent.ExecutionException

import cats.data.Reader
import com.sky.kafka.configurator.error.TopicNotFound
import org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.admin._
import org.apache.kafka.common.config.ConfigResource
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException
import org.zalando.grafter.{ Stop, StopResult }

import scala.collection.JavaConverters._
import scala.language.postfixOps
import scala.util.{ Failure, Success, Try }

object KafkaTopicAdmin {
  def apply(adminClient: AdminClient): KafkaTopicAdmin = new KafkaTopicAdmin(adminClient)

  def reader: Reader[AppConfig, KafkaTopicAdmin] = Reader { config =>
    import com.sky.kafka.utils.MapToJavaPropertiesConversion.mapToProperties
    KafkaTopicAdmin(AdminClient.create(Map(BOOTSTRAP_SERVERS_CONFIG -> config.bootstrapServers) ++ config.props))
  }
}

class KafkaTopicAdmin(ac: AdminClient) extends TopicReader with TopicWriter with Stop {

  override def fetch(topicName: String) = {

    def topicDescription = Try {
      val allDescriptions = ac.describeTopics(Seq(topicName).asJava).all.get
      allDescriptions.get(topicName)
    } match {
      case Success(result) => Success(result)
      case Failure(e: ExecutionException) if e.getCause.isInstanceOf[UnknownTopicOrPartitionException] => Failure(TopicNotFound(topicName))
      case other => other
    }

    def topicConfig = Try {
      val allConfigs = ac.describeConfigs(Seq(configResourceForTopic(topicName)).asJava).all.get
      allConfigs.get(configResourceForTopic(topicName))
    }

    for {
      desc <- topicDescription
      partitions = desc.partitions().size()
      replicationFactor = desc.partitions().asScala.head.replicas().size()
      config <- topicConfig
    } yield Topic(desc.name(), partitions, replicationFactor, config)

  }

  override def create(topic: Topic) = Try {
    val newTopic = new NewTopic(topic.name, topic.partitions, topic.replicationFactor.toShort).configs(topic.config.asJava)
    ac.createTopics(Seq(newTopic).asJava).all().get
  }

  override def updateConfig(topicName: String, config: Map[String, Object]) = Try {
    val c = config.map {
      case (key, value) => new ConfigEntry(key, value.toString)
    }.toList.asJava
    ac.alterConfigs(Map(configResourceForTopic(topicName) -> new Config(c)).asJava).all().get
  }

  override def updatePartitions(topicName: String, numPartitions: Int) = Try {
    ac.createPartitions(Map(topicName -> NewPartitions.increaseTo(numPartitions)).asJava).all().get()
  }

  override def stop = StopResult.eval("KafkaAdminClient")(ac.close())

  private def configResourceForTopic(topicName: String) = new ConfigResource(ConfigResource.Type.TOPIC, topicName)

  private implicit def kafkaConfigToMap(config: Config): Map[String, String] = config.entries().asScala.map { entry =>
    entry.name() -> entry.value()
  } toMap
}


