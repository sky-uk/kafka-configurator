package com.sky.kafka.configurator

import java.io.{Reader => JReader}

import io.circe.yaml.parser.parse

import scala.util.Try

object TopicConfigurationParser {
  def apply(topicConfigReader: JReader): Try[List[Topic]] =
    for {
      ymlAsJson <- parse(topicConfigReader).toTry
      topicConfigs <- ymlAsJson.as[List[Topic]].toTry
    } yield topicConfigs
}
