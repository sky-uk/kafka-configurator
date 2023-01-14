package common

import com.pirum.kafka.testkit.KafkaServer
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.time.{Millis, Seconds, Span}

import scala.collection.JavaConverters._

abstract class KafkaIntSpec extends BaseSpec with BeforeAndAfterAll with PatienceConfiguration {

  override implicit val patienceConfig = PatienceConfig(Span(3, Seconds), Span(250, Millis))

  val kafkaServer = new KafkaServer()
  val kafkaPort   = kafkaServer.kafkaPort

  lazy val kafkaAdminClient = AdminClient.create(
    Map[String, AnyRef](
      BOOTSTRAP_SERVERS_CONFIG -> s"localhost:$kafkaPort"
    ).asJava
  )

  override def beforeAll() = kafkaServer.startup()

  override def afterAll() = {
    kafkaAdminClient.close()
    kafkaServer.close()
  }

}
