FROM        anapsix/alpine-java:8
MAINTAINER  The Kafka Configurator Authors

ENV KC_VERSION 0.2.0
ENV ARCHIVE_NAME kafka-configurator-$KC_VERSION

RUN apk add --update curl && \
    rm -rf /var/cache/apk/*
RUN curl -L https://bintray.com/sky-uk/oss-maven/download_file?file_path=com%2Fsky%2Fkafka-configurator%2F$KC_VERSION%2F$ARCHIVE_NAME.tgz > /opt/kc.tgz
RUN tar -xf /opt/kc.tgz -C /opt && mv /opt/$ARCHIVE_NAME /opt/kafka-configurator
RUN chmod +x /opt/kafka-configurator/bin/kafka-configurator
RUN chmod +x /opt/kafka-configurator/bin/kafka-configurator.bat

ENTRYPOINT ["/opt/kafka-configurator/bin/kafka-configurator"]
