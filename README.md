# Kafka Configurator

[![Build Status](https://travis-ci.org/sky-uk/kafka-configurator.svg?branch=master)](https://travis-ci.org/sky-uk/kafka-configurator)
[![Download](https://api.bintray.com/packages/sky-uk/oss-maven/kafka-configurator/images/download.svg)](https://bintray.com/sky-uk/oss-maven/kafka-configurator/_latestVersion)

Command line tool to create and update Kafka topics based on the provided configuration.

This software can be used as one of two ways:

 - As a standalone tool for automatically creating topics and updating their parameters. It reads a YAML description of the desired setup, compares it with the current state and alters the topics that are different.
 
 - As a dependency that can be added to your code-base as a library that allows you to manage kafka-topics within your application.


## Usage as a standalone program
### Download

#### Binary archive

Released artifacts are published to [Bintray](https://bintray.com/sky-uk/oss-maven/kafka-configurator/_latestVersion#files) as zip or tgz archives.

It does not require an installation process: just extract the archive into any directory and execute `bin/kafka-configurator` (or `bin\kafka-configurator.bat` on Windows) to see the usage instructions.

#### Docker Image

The Docker Image is available from Docker Hub at [skyuk/kafka-configurator](https://hub.docker.com/r/skyuk/kafka-configurator)


```
Usage: kafka-configurator [options]

  -f, --files <file1>,<file2>...
                           Topic configuration files
  --bootstrap-servers <value>
                           Kafka brokers URLs for bootstrap (comma-separated)
  --properties <value>     Kafka admin client config as comma-separated pairs
```

The topic configuration file has the following format:
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

The root items are topic names to be created or updated, and contain their configuration parameters: `partitions` and `replication` are integers, while the `config` block accepts any valid [topic-level configuration](https://kafka.apache.org/documentation/#topicconfigs). We let Kafka validate these configurations for us so we don't have to explicitly support each topic-level configuration.

### Demo

Create a `test-topics.yml` file with the contents of the example configuration above.

##### Using the extracted binary

Start Kafka and Zookeeper using two separate shells in the Kafka root directory:
```
1$ bin/zookeeper-server-start.sh config/zookeeper.properties
2$ bin/kafka-server-start.sh config/server.properties
```

Execute the Kafka Configurator:
```
$ bin/kafka-configurator -f test-topics.yml --zookeeper localhost:2181
```

Query the topics using the CLI tool bundled with Kafka:
```
$ bin/kafka-topics.sh --zookeeper localhost:2181 --describe
Topic:topic1    PartitionCount:10       ReplicationFactor:1     Configs:retention.ms=0,delete.retention.ms=86400000,min.insync.replicas=2,cleanup.policy=compact
        ...
Topic:topic2    PartitionCount:5        ReplicationFactor:1     Configs:retention.ms=86400000,delete.retention.ms=0,min.insync.replicas=2,cleanup.policy=delete
        ...
```

Any changes to the `test-topics.yml` file will be applied to the existing topics at each subsequent run.

##### Using the Docker image

Assuming you know the `<zookeeper_address>` and have placed your config file named `test-topics.yml` inside the `<config_dir_on_host>` directory on the Docker host, an example of how to run the image is:

`docker run -it -v <config_dir_on_host>:/etc/kafka-configurator skyuk/kafka-configurator -f=/etc/kafka-configurator/test-topics.yml --zookeeper=<zookeeper_address>`

Alternatively you could extend the `skyuk/kafka-configurator` image and `COPY` your configuration file directly into your extended image.

##### Injecting Kafka Admin client config

Any [Kafka Admin client config](http://kafka.apache.org/documentation/#adminclientconfigs) is supported, both when running the binary directly or via Docker. 

These can be passed in the `--properties` command line option as key=value comma separated pairs:

`kafka-configurator -f test-topics.yml --zookeeper localhost:2181 --properties client.id=foo,ssl.key.password=bar`

Or they can be injected as environment variables prefixed with `KAFKA_`:

`docker run -it -v <config_dir_on_host>:/etc/kafka-configurator -e KAFKA_CLIENT_ID=foo -e KAFKA_SSL_KEY_PASSWORD=bar skyuk/kafka-configurator -f=/etc/kafka-configurator/test-topics.yml --zookeeper=<zookeeper_address>`

## Usage for adding as a dependency

##### SBT
```sbtshell
useJCenter := true

libraryDependencies += "com.sky" %% "kafka-configurator" % "VERSION"

```

##### Gradle
```groovy
repositories {
    jcenter()
}

compile 'com.sky:kafka-configurator_2.12:VERSION'
```

##### Maven
```xml
<repositories>
    <repository>
        <id>jcenter</id>
        <name>jcenter</name>
        <url>http://jcenter.bintray.com</url>
    </repository>
</repositories>

<dependency>
  <groupId>com.sky</groupId>
  <artifactId>kafka-configurator_2.12</artifactId>
  <version>VERSION</version>
</dependency>
```

### Scala Example:
```scala
val config = AppConfig(files = Seq(new File("topics.yml")), bootstrapServers = "examplekafka.com:9092")
val result = KafkaConfiguratorApp.reader(config).configureTopicsFrom(config.files.toList)

result match {
  case Success((Nil, logs)) => logger.info(s"Topics successfully configured. $logs")
  case Success((errors, logs)) => logger.error(s"Configurator errors: $errors, $logs")
  case Failure(e) => logger.error("Kafka Configurator has failed", e)
}   
```    
