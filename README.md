# Kafka Configurator

[![Build Status](https://travis-ci.org/sky-uk/kafka-configurator.svg?branch=master)](https://travis-ci.org/sky-uk/kafka-configurator)

Command line tool to create and update Kafka topics based on the provided configuration.

This software is meant to be used as a tool for automatically creating topics and updating their parameters.
It reads a YAML description of the desired setup, compares it with the current state and alters the topics
that are different.

```
Usage: kafka-configurator [options]

  -f, --file <file>        Topic configuration file
  --zookeeper <value>      Zookeeper URLs (comma-separated)
  --zookeeper-timeout <value>
                           Session and connection timeout for Zookeeper
```

The `config` block in the topic configuration file accepts any valid [topic-level configuration](https://kafka.apache.org/documentation/#topic-config). We let Kafka validate these configurations for us so we don't have to explicitly support each topic-level configuration.  

Example topic configuration file:
```yaml
topic1:
  partitions: 10
  replication: 3
  config:
    cleanup.policy: compact
    delete.retention.ms: 86400000
    min.compaction.lag.ms: 21600000
    retention.ms: 0
    min.insync.replicas: 3

topic2:
  partitions: 5
  replication: 2
  config:
    cleanup.policy: delete
    delete.retention.ms: 0
    retention.ms: 604800000
    min.insync.replicas: 2
```

## How to build

The software is written in Scala and is build with SBT.

To create the executable in the `target/universal/stage` directory:
```
sbt ';test ;stage'
```

To create the release zip file in the `target/universal` directory:
```
sbt packageBin
```

### Demo

Start Kafka and Zookeeper using two separate shells in the Kafka root directory:
```
1$ bin/zookeeper-server-start.sh config/zookeeper.properties
2$ bin/kafka-server-start.sh config/server.properties
```

Execute the Kafka Configurator from the source directory:
```
$ sbt ';test ;stage' # only the first time
$ target/universal/stage/bin/kafka-configurator -f src/test/resources/topic-configuration.yml --zookeeper localhost:2181
```

Query the topics using the CLI tool bundled with Kafka:
```
$ bin/kafka-topics.sh --zookeeper localhost:2181 --describe
Topic:topic1    PartitionCount:10       ReplicationFactor:1     Configs:retention.ms=0,delete.retention.ms=86400000,min.insync.replicas=2,cleanup.policy=compact
        ...
Topic:topic2    PartitionCount:5        ReplicationFactor:1     Configs:retention.ms=86400000,delete.retention.ms=0,min.insync.replicas=2,cleanup.policy=delete
        ...
```
