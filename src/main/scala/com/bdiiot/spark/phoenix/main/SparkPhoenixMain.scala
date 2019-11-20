package com.bdiiot.spark.phoenix.main

import java.sql.{Connection, DriverManager, ResultSet, Statement}

import com.bdiiot.spark.phoenix.utils.Constant._
import com.bdiiot.spark.phoenix.utils.SparkHelper
import org.apache.spark.sql
import org.apache.spark.sql.ForeachWriter


object SparkPhoenixMain {
  def main(args: Array[String]): Unit = {
    if (SECURITY == "SASL_PLAINTEXT") {
      System.setProperty("java.security.krb5.conf", "/etc/krb5.conf")
      System.setProperty("java.security.auth.login.config", "/tmp/kafka_bigdata_jaas.conf")
    }

    val spark = SparkHelper.getSparkSession()

    val kafkaSource: sql.DataFrame = spark
      .readStream
      .format(KAFKA_SOURCE)
      .option("kafka.bootstrap.servers", BROKERS)
      .option("subscribe", TOPICS)
      .option("kafka.security.protocol", SECURITY)
      .option("startingOffsets", OFFSETS)
      .load()

    import spark.implicits._
    val kafkaSourceString = kafkaSource.selectExpr("CAST(value AS STRING)").as[String]

    val query = kafkaSourceString.writeStream
      .foreach(new ForeachWriter[String] {
        var connection: Connection = _
        var statement: Statement = _
        var resultSet: ResultSet = _

        override def open(partitionId: Long, version: Long): Boolean = {
          //          val config: Configuration = HBaseConfiguration.create()
          //          config.addResource(new Path(CORE_SITE))
          //          config.addResource(new Path(HDFS_SITE))
          //          config.addResource(new Path(HBASE_SITE))
          //
          //          val configuration: Configuration = new Configuration()
          //          configuration.set("hadoop.security.authentication", "Kerberos")
          //
          //          UserGroupInformation.setConfiguration(configuration)
          //          UserGroupInformation.loginUserFromKeytab(USER, KEYTAB)

          val jdbc_url = "jdbc:phoenix:h11.bdiiot.com,h12.bdiiot.com,h13.bdiiot.com:2181:/hbase-secure:hbase-bdiiot@BDIIOT.COM:/etc/security/keytabs/hbase.headless.keytab"
          Class.forName("org.apache.phoenix.jdbc.PhoenixDriver")
          connection = DriverManager.getConnection(jdbc_url)
          statement = connection.createStatement
          true
        }

        override def process(value: String): Unit = {
          var sql = "upsert into test.test_phoenix values (1,'a')"
          statement.executeUpdate(sql)
          connection.commit()

          sql = "select * from test.test_phoenix"
          val resultSet = statement.executeQuery(sql)
          while (resultSet.next()) {
            println(resultSet.getObject(1).toString)
          }
        }

        override def close(errorOrNull: Throwable): Unit = {
          try {
            resultSet.close()
          } catch {
            case ex: Exception => {
              println(s"close resultSet failed, msg=$ex")
            }
          }
          try {
            statement.close()
          } catch {
            case ex: Exception => {
              println(s"close statement failed, msg=$ex")
            }
          }
          try {
            connection.close()
          } catch {
            case ex: Exception => {
              println(s"close connection failed, msg=$ex")
            }
          }
        }
      })
      .outputMode(OUTPUT_MODE)
      .option("checkpointLocation", PATH_CHECKPOINT + "demo")
      .start()

    query.awaitTermination()
    SparkHelper.close
  }
}
