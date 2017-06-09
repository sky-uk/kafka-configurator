package com.sky.kafka.configurator

import cats.Eval
import cats.data.Reader
import com.sky.kafka.utils.MapToJavaPropertiesConversion._
import kafka.admin.AdminUtils
import kafka.server.ConfigType
import kafka.utils.ZkUtils
import org.apache.kafka.common.errors.InvalidPartitionsException
import org.apache.kafka.common.security.JaasUtils
import org.zalando.grafter._

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

case class KafkaAdminClient(zkUtils: ZkUtils) extends TopicReader with TopicWriter with Stop {

  type Partitions = Map[Int, Seq[Int]]

  override def create(topic: Topic): Try[Unit] = Try {
    import com.sky.kafka.utils.MapToJavaPropertiesConversion._
    AdminUtils.createTopic(
      zkUtils,
      topic.name,
      topic.partitions,
      topic.replicationFactor,
      topic.config
    )
  }

  override def updateConfig(topicName: String, config: Map[String, Object]): Try[Unit] = Try {
    AdminUtils.changeTopicConfig(zkUtils, topicName, config)
  }

  override def updatePartitions(topicName: String, numPartitions: Int): Try[Unit] = Try {
    AdminUtils.addPartitions(zkUtils, topicName, numPartitions)
  }

  override def fetch(topicName: String): Try[Topic] =
    for {
      partitions <- fetchPartitionsFor(topicName)
      replicationFactor <- replicationFactorFrom(partitions)
      topicConfig <- fetchEntityConfig(topicName)
    } yield Topic(topicName, partitions.size, replicationFactor, topicConfig)

  private def fetchPartitionsFor(topicName: String): Try[Partitions] =
    Try(zkUtils.getPartitionAssignmentForTopics(Seq(topicName))).flatMap(_.get(topicName) match {
      case Some(partitions) if partitions.nonEmpty => Success(partitions.toMap)
      case _ => Failure(TopicNotFound(topicName))
    })

  private def replicationFactorFrom(partitions: Partitions): Try[Int] = partitions.headOption match {
    case Some((_, replicas)) => Success(replicas.length)
    case None => Failure(new InvalidPartitionsException("No partitions found"))
  }

  private def fetchEntityConfig(topicName: String): Try[Map[String, String]] = Try {
    AdminUtils.fetchEntityConfig(zkUtils, ConfigType.Topic, topicName).asScala.toMap
  }

  override def stop: Eval[StopResult] = StopResult.eval("zkUtils")(zkUtils.close())
}

object KafkaAdminClient {
  def reader: Reader[AppConfig, KafkaAdminClient] = ZkConfig.reader.map { zk =>
    val zkUtils = ZkUtils(zk.urls, zk.timeout, zk.timeout, JaasUtils.isZkSecurityEnabled)
    KafkaAdminClient(zkUtils)
  }
}
