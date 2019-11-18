#!/bin/bash

hadoop fs -rm -r -f /tmp/spark_phoenix

export SPARK_MAJOR_VERSION=2

spark-submit \
--master yarn \
--deploy-mode client \
--conf "spark.executor.extraJavaOptions=-Djava.security.auth.login.config=/tmp/kafka_bigdata_jaas.conf" \
--class com.bdiiot.spark.phoenix.main.SparkPhoenixMain \
spark-phoenix-demo-1.0-SNAPSHOT.jar