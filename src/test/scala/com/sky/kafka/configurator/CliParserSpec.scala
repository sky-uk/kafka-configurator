package com.sky.kafka.configurator

import java.io.{ByteArrayOutputStream, File}

import com.sky.kafka.configurator.error.InvalidArgsException
import common.BaseSpec

class CliParserSpec extends BaseSpec {

  val BootstraServers = "kafka1:2181,kafka2:2195"

  "parse" should "successfully parse valid command line args" in {
    val configFilePath: String = getClass.getResource("/topic-configuration.yml").getPath
    val args = Array(
      "-f", configFilePath,
      "--bootstrap-servers", BootstraServers
    )

    val (parsed, _, _) = captureOutErr {
      Main.parse(args)
    }

    parsed.success.value shouldBe AppConfig(new File(configFilePath), BootstraServers)
  }

  it should "fail when required args are missing" in {
    val args = Array.empty[String]

    val (parsed, _, err) = captureOutErr {
      Main.parse(args)
    }

    parsed.failure.exception shouldBe InvalidArgsException
    err should include("Missing option --file")
    err should include("Missing option --bootstrap-servers")
    err should include("Usage:")
  }

  it should "fail when the file arg provided does not exist" in {
    val fileThatDoesNotExist = "doesNotExist"
    val args = Array(
      "-f", fileThatDoesNotExist,
      "--bootstrap-servers", BootstraServers
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
