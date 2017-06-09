package com.sky.kafka

import java.io.File

import cats.data.Reader
import cats.implicits._
import io.circe.Decoder._
import io.circe.generic.auto._
import io.circe.{Decoder, DecodingFailure, Json}

import scala.util.Try

package object configurator {

  case class AppConfig(file: File = new File("."), zk: ZkConfig = ZkConfig())

  case class ZkConfig(urls: String = "", timeout: Int = 30000)

  object ZkConfig {
    def reader: Reader[AppConfig, ZkConfig] = Reader(_.zk)
  }

  case class Topic(name: String, partitions: Int, replicationFactor: Int, config: Map[String, String])

  case class TopicNotFound(topicName: String) extends Exception(s"$topicName not found")

  case object ReplicationChangeFound extends Exception("Changing replication factor is unsupported")

  case class TopicConfig(partitions: Int, replication: Int, config: Map[String, String])

  trait TopicReader {
    def fetch(topicName: String): Try[Topic]
  }

  trait TopicWriter {
    def create(topic: Topic): Try[Unit]

    def updateConfig(topicName: String, config: Map[String, Object]): Try[Unit]

    def updatePartitions(topicName: String, numPartitions: Int): Try[Unit]
  }

  implicit val decodeStringMap: Decoder[Map[String, String]] = Decoder.instance { cursor =>
    def stringify(json: Json): Json = json.asNumber
      .map(num => Json.fromString(num.truncateToInt.toString))
      .getOrElse(json)

    def failWithMsg(msg: String) = DecodingFailure(msg, List.empty)

    for {
      jsonObj <- cursor.value.asObject.toRight(failWithMsg(s"${cursor.value} is not an object"))
      valuesAsJsonStrings = jsonObj.withJsons(stringify).toMap
      stringMap <- valuesAsJsonStrings
        .mapValues(json => json.asString.toRight(failWithMsg(s"$json is not a string")))
        .sequenceU
    } yield stringMap
  }

  implicit val decodeTopics: Decoder[List[Topic]] = Decoder.instance { cursor =>
    for {
      configMap <- cursor.as[Map[String, TopicConfig]]
      topics = configMap.map { case (name, conf) => Topic(name, conf.partitions, conf.replication, conf.config) }
    } yield topics.toList
  }
}
