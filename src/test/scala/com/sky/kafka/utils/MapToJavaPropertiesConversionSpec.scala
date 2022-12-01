package com.sky.kafka.utils

import java.math.BigDecimal
import java.util.Properties

import common.BaseSpec

class MapToJavaPropertiesConversionSpec extends BaseSpec {

  "mapToProperties" should "convert objects into their string representation" in {
    MapToJavaPropertiesConversion.mapToProperties(
      Map[String, Object](
        "object" -> new BigDecimal("123.456")
      )
    ) shouldBe new Properties {
      setProperty("object", "123.456")
    }
  }

  it should "convert classes into the full class name" in {
    MapToJavaPropertiesConversion.mapToProperties(
      Map[String, Object](
        "class" -> classOf[Exception]
      )
    ) shouldBe new Properties {
      setProperty("class", "java.lang.Exception")
    }
  }
}
