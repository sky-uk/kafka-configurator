package common

import cakesolutions.kafka.testkit.KafkaServer
import kafka.utils.ZkUtils
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.time.{ Millis, Seconds, Span }

import scala.collection.JavaConverters._
import scala.concurrent.duration._

abstract class KafkaIntSpec extends BaseSpec with BeforeAndAfterAll with PatienceConfiguration {

  override implicit val patienceConfig = PatienceConfig(Span(3, Seconds), Span(250, Millis))

  val kafkaServer = new KafkaServer()
  val kafkaPort = kafkaServer.kafkaPort

  val zkSessionTimeout = 30 seconds
  val zkConnectionTimeout = 30 seconds

  lazy val zkUtils = ZkUtils(s"localhost:${kafkaServer.zookeeperPort}", zkSessionTimeout.toMillis.toInt,
    zkConnectionTimeout.toMillis.toInt, isZkSecurityEnabled = false)

  lazy val kafkaAdminClient = AdminClient.create(Map[String, AnyRef](
    BOOTSTRAP_SERVERS_CONFIG -> s"localhost:$kafkaPort"
  ).asJava)

  val defaultTopicProperties = Map(
    "message.timestamp.difference.max.ms" -> "9223372036854775807",
    "max.message.bytes" -> "1000012",
    "segment.index.bytes" -> "10485760",
    "segment.jitter.ms" -> "0",
    "min.cleanable.dirty.ratio" -> "0.5",
    "retention.bytes" -> "-1",
    "follower.replication.throttled.replicas" -> "",
    "file.delete.delay.ms" -> "60000",
    "compression.type" -> "producer",
    "min.compaction.lag.ms" -> "0",
    "flush.ms" -> "9223372036854775807",
    "cleanup.policy" -> "delete",
    "message.timestamp.type" -> "CreateTime",
    "unclean.leader.election.enable" -> "false",
    "flush.messages" -> "9223372036854775807",
    "retention.ms" -> "604800000",
    "min.insync.replicas" -> "1",
    "message.format.version" -> "1.1-IV0",
    "leader.replication.throttled.replicas" -> "",
    "delete.retention.ms" -> "86400000",
    "preallocate" -> "false",
    "index.interval.bytes" -> "4096",
    "segment.bytes" -> "1073741824",
    "segment.ms" -> "604800000"
  )


  override def beforeAll() = kafkaServer.startup()

  override def afterAll() = {
    kafkaAdminClient.close()
    kafkaServer.close()
  }

}