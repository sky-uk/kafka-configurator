package com.sky.kafka.configurator

import java.io.{ByteArrayOutputStream, File}

import com.sky.kafka.configurator.error.InvalidArgsException
import common.BaseSpec

class CliParserSpec extends BaseSpec {

  "parse" should "successfully parse valid command line args" in {
    val configFilePath: String = getClass.getResource("/topic-configuration.yml").getPath
    val args = Array(
      "-f", configFilePath,
      "--zookeeper", "zk1:2181,zk2:2195",
      "--zookeeper-timeout", "10000"
    )

    val (parsed, _, _) = captureOutErr {
      Main.parse(args)
    }

    parsed.success.value shouldBe AppConfig(new File(configFilePath), ZkConfig("zk1:2181,zk2:2195", 10000))
  }

  it should "fail when required args are missing" in {
    val args = Array.empty[String]

    val (parsed, _, err) = captureOutErr {
      Main.parse(args)
    }

    parsed.failure.exception shouldBe InvalidArgsException
    err should include("Missing option --file")
    err should include("Missing option --zookeeper")
    err should include("Usage:")
    err should not include "Missing option --zookeeper-timeout"
  }

  it should "fail when the file arg provided does not exist" in {
    val fileThatDoesNotExist = "doesNotExist"
    val args = Array(
      "-f", fileThatDoesNotExist,
      "--zookeeper", "zk1:2181,zk2:2195",
      "--zookeeper-timeout", "10000"
    )

    val (parsed, _, err) = captureOutErr {
      Main.parse(args)
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
}
