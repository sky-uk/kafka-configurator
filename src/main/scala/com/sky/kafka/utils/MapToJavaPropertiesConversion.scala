package com.sky.kafka.utils

import java.util.Properties

object MapToJavaPropertiesConversion {
  implicit def mapToProperties(inputMap: Map[String, Object]): Properties = {
    def stringify(v: Object): String = v match {
      case c: Class[_] => c.getName
      case _ => v.toString
    }

    (new Properties /: inputMap) {
      case (a, (k, v)) =>
        a.put(k, stringify(v))
        a
    }
  }

}
