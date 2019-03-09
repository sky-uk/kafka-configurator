package com.sky.kafka.configurator

import java.io.{ByteArrayOutputStream, File}

import com.sky.kafka.configurator.error.InvalidArgsException
import common.BaseSpec

import scala.util.Try

class ConfigParsingSpec extends BaseSpec {

  "parse" should "successfully parse valid command line args" in new TestContext {
    val adminClientConfig = Map("ssl.key.password" -> "password", "client.id" -> "some-client")
    val args = buildArgsFrom(configFilePath, BootstrapServers, adminClientConfig)

    runParser(args, Map.empty).success.value shouldBe expectedAppConfig(adminClientConfig)
  }

  it should "support reading Kafka config from sys env" in new TestContext {
    val args = buildArgsFrom(configFilePath, BootstrapServers)

    runParser(args, Map(supportEnvVarKeyFrom(validConfigKey) -> someConfigValue))
      .success.value shouldBe expectedAppConfig(Map(validConfigKey -> someConfigValue))
  }

  it should "prioritise sys env over command line for Kafka config" in new TestContext {
    val args = buildArgsFrom(configFilePath, BootstrapServers, Map(validConfigKey -> someConfigValue))

    runParser(args, Map(supportEnvVarKeyFrom(validConfigKey) -> someConfigValue))
      .success.value shouldBe expectedAppConfig(Map(validConfigKey -> someConfigValue))
  }

  it should "only support sys env values prefixed with KAFKA_" in new TestContext {
    val args = buildArgsFrom(configFilePath, BootstrapServers)

    runParser(args, Map(supportEnvVarKeyFrom(validConfigKey) -> someConfigValue, "ssl.keystore.password" -> "foo"))
      .success.value shouldBe expectedAppConfig(Map(validConfigKey -> someConfigValue))
  }

  it should "drop properties/env vars that are not valid Kafka admin client configs" in new TestContext {
    val args = buildArgsFrom(configFilePath, BootstrapServers, Map(validConfigKey -> someConfigValue, "invalid" -> "invalid"))

    runParser(args, Map("KAFKA_CLIENT_ID" -> someConfigValue, "another-invalid" -> "invalid"))
      .success.value shouldBe expectedAppConfig(Map(validConfigKey -> someConfigValue, "client.id" -> someConfigValue))
  }

  it should "fail when required args are missing" in {
    val args = Array.empty[String]

    val (parsed, _, err) = captureOutErr {
      ConfigParsing.parse(args, Map.empty)
    }

    parsed.failure.exception shouldBe InvalidArgsException
    err should include("Missing option --file")
    err should include("Missing option --bootstrap-servers")
    err should include("Usage:")
  }

  it should "fail when the file arg provided does not exist" in new TestContext {
    val fileThatDoesNotExist = "doesNotExist"

    val (parsed, _, err) = captureOutErr {
      ConfigParsing.parse(buildArgsFrom(fileThatDoesNotExist, BootstrapServers, Map.empty), Map.empty)
    }

    parsed.failure.exception shouldBe InvalidArgsException
    err should include(s"$fileThatDoesNotExist does not exist.")
  }

  def captureOutErr[T](f: => T): (T, String, String) = {
    val outCapture = new ByteArrayOutputStream
    val errCapture = new ByteArrayOutputStream

    val t = Console.withOut(outCapture) {
      Console.withErr(errCapture) {
        f
      }
    }

    (t, outCapture.toString, errCapture.toString)
  }

  private class TestContext {
    val BootstrapServers = "kafka1:2181,kafka2:2195"
    val configFilePath = getClass.getResource("/topic-configuration.yml").getPath
    val validConfigKey = "ssl.key.password"
    val someConfigValue = "password"

    def commaSepStringFrom(stringMap: Map[String, String]): String =
      stringMap.map { case (k, v) => s"$k=$v" }.mkString(",")

    def buildArgsFrom(filePath: String, bootstrapServers: String, properties: Map[String, String] = Map.empty): Array[String] =
      Array(
        "-f", filePath,
        "--bootstrap-servers", bootstrapServers
      ) ++ {
        if (properties.isEmpty) Array.empty[String] else Array("--properties", commaSepStringFrom(properties))
      }

    def runParser(args: Array[String], props: Map[String, String]): Try[AppConfig] =
      captureOutErr {
        ConfigParsing.parse(args, props)
      }._1

    def supportEnvVarKeyFrom(key: String): String = "KAFKA_" + key.toUpperCase.replace(".", "_")

    def expectedAppConfig(props: Map[String, String]) = AppConfig(new File(configFilePath), BootstrapServers, props)
  }

}
