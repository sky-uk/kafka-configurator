package com.sky.kafka.configurator

import java.io.{Reader => JReader}

import cats.instances.either._
import cats.instances.list._
import cats.syntax.traverse._
import io.circe
import io.circe.generic.AutoDerivation
import io.circe.yaml.parser._
import io.circe.{Decoder, DecodingFailure, Json}

import scala.collection.immutable.ListMap

object TopicConfigurationParser extends AutoDerivation {

  def apply(topicConfigReader: JReader): Either[circe.Error, List[Topic]] =
    for {
      ymlAsJson <- parse(topicConfigReader)
      topicConfigs <- ymlAsJson.as[List[Topic]]
    } yield topicConfigs

  case class TopicConfig(partitions: Int, replication: Int, config: Map[String, String])

  implicit val topicsDecoder: Decoder[List[Topic]] = Decoder.instance { cursor =>
    for {
      configMap <- cursor.as[ListMap[String, TopicConfig]]
      topics = configMap.map { case (name, conf) => Topic(name, conf.partitions, conf.replication, conf.config) }
    } yield topics.toList
  }

  implicit val stringMapDecoder: Decoder[Map[String, String]] = Decoder.instance { cursor =>
    def stringify(json: Json): Json = json.asNumber
      .flatMap(_.toInt)
      .map(n => Json.fromString(n.toString))
      .getOrElse(json)

    def failWithMsg(msg: String) = DecodingFailure(msg, List.empty)

    for {
      jsonObj <- cursor.value.asObject.toRight(failWithMsg(s"${cursor.value} is not an object"))
      valuesAsJsonStrings = jsonObj.mapValues(stringify)
      stringMap <- valuesAsJsonStrings.toList.traverse[Decoder.Result, (String, String)] {
        case (key, json) => json.asString.toRight(failWithMsg(s"$json is not a string")).map(key -> _)
      }
    } yield stringMap.toMap
  }
}
