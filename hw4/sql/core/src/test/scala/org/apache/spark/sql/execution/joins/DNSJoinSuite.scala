package org.apache.spark.sql.execution.joins

import java.util.{ArrayList => JavaArrayList}

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.catalyst.ScalaReflection
import org.apache.spark.sql.catalyst.expressions.Attribute
import org.apache.spark.sql.execution.joins.dns.GeneralDNSJoin
import org.apache.spark.sql.execution.{PhysicalRDD, SparkPlan}
import org.apache.spark.sql.test.TestSQLContext._
import org.scalatest.FunSuite

import scala.util.Random

case class IP (ip: String)

class DNSJoinSuite extends FunSuite {
  val random: Random = new Random

  // initialize Spark magic stuff that we don't need to care about
  val sqlContext = new SQLContext(sparkContext)
  val IPAttributes: Seq[Attribute] = ScalaReflection.attributesFor[IP]

  var createdIPs: JavaArrayList[IP] = new JavaArrayList[IP]()
  for ( i <- 1 to 100){
    val ip: IP = IP((random.nextInt(220) + 1) + "." + random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256))
    createdIPs.add(ip)
    createdIPs.add(ip)
  }
  import sqlContext.createSchemaRDD

  // initialize a SparkPlan that is a sequential scan over a small amount of data
  val smallRDD1: RDD[IP] = sparkContext.parallelize((1 to 200).map(i => {
    val ip: IP = createdIPs.get(i-1)
    ip
  }), 1)
  val smallScan1: SparkPlan = PhysicalRDD(IPAttributes, smallRDD1)

  test ("simple dns join") {
    val outputRDD = GeneralDNSJoin(IPAttributes, IPAttributes, smallScan1, smallScan1).execute()

    val result = outputRDD.collect
    assert(result.length == 200)

    result.foreach(x => {
      println(x)
      val ip = IP(x.getString(0))
      assert(createdIPs contains ip)
      createdIPs remove ip
    })
  }
}
