package com.sky.kafka.configurator.config

import java.io.File

case class Config(
    files: Seq[File] = Seq.empty,
    bootstrapServers: String = "",
    props: Map[String, String] = Map.empty
)
