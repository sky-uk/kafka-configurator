package common

import cakesolutions.kafka.testkit.KafkaServer
import kafka.utils.ZkUtils
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.duration._

abstract class KafkaIntSpec extends BaseSpec with BeforeAndAfterAll with PatienceConfiguration {

  override implicit val patienceConfig = PatienceConfig(Span(3, Seconds), Span(250, Millis))

  val kafkaServer = new KafkaServer()
  val kafkaPort = kafkaServer.kafkaPort

  val zkSessionTimeout = 30 seconds
  val zkConnectionTimeout = 30 seconds

  lazy val zkUtils = ZkUtils(s"localhost:${kafkaServer.zookeeperPort}", zkSessionTimeout.toMillis.toInt,
    zkConnectionTimeout.toMillis.toInt, isZkSecurityEnabled = false)

  override def beforeAll() = kafkaServer.startup()

  override def afterAll() = {
    zkUtils.close()
    kafkaServer.close()
  }

}