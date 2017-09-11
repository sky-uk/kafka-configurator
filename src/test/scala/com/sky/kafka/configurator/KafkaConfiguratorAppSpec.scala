package com.sky.kafka.configurator

import java.io.{File, FileReader}

import com.sky.kafka.configurator.error.{ConfiguratorFailure, TopicNotFound}
import common.BaseSpec
import io.circe.generic.AutoDerivation
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar

import scala.util.{Failure, Success}

class KafkaConfiguratorAppSpec extends BaseSpec with MockitoSugar with AutoDerivation {

  val topicConfigurator = mock[TopicConfigurator]
  val kafkaConfiguratorApp = KafkaConfiguratorApp(topicConfigurator)

  it should "provide logs and errors when file has been parsed successfully" in {
    val file = new File(getClass.getResource("/topic-configuration-with-error.yml").getPath)
    val topics = TopicConfigurationParser(new FileReader(file)).right.get

    val error = TopicNotFound(topics(1).name)

    when(topicConfigurator.configure(topics.head))
      .thenReturn(Success(()).withLog("foo"))
    when(topicConfigurator.configure(topics(1)))
      .thenReturn(Failure[Unit](error).asWriter)
    when(topicConfigurator.configure(topics(2)))
      .thenReturn(Success(()).withLog("bar"))

    kafkaConfiguratorApp.configureTopicsFrom(file) shouldBe Success((
      List(ConfiguratorFailure(topics.tail.head.name, error)),
      List("foo", "bar")
    ))
  }

  it should "fail-fast when the file does not exist" in {
    kafkaConfiguratorApp.configureTopicsFrom(new File("does-not-exist")) shouldBe a[Failure[_]]
  }

  it should "fail-fast when given an invalid file" in {
    val invalidFile = File.createTempFile("invalid", "file")
    invalidFile.deleteOnExit()
    kafkaConfiguratorApp.configureTopicsFrom(invalidFile) shouldBe a[Failure[_]]
  }

}
