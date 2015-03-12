package org.apache.spark.sql.execution.joins

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.catalyst.ScalaReflection
import org.apache.spark.sql.catalyst.expressions.Attribute
import org.apache.spark.sql.execution.joins.dns.GeneralDNSJoin
import org.apache.spark.sql.execution.{PhysicalRDD, SparkPlan}
import org.apache.spark.sql.test.TestSQLContext._
import org.scalatest.FunSuite

import scala.collection.mutable.HashSet
import scala.util.Random

case class IP (ip: String)
case class ReverseDNS

class DNSJoinSuite extends FunSuite {
  val random: Random = new Random

  // initialize Spark magic stuff that we don't need to care about
  val sqlContext = new SQLContext(sparkContext)
  val IPAttributes: Seq[Attribute] = ScalaReflection.attributesFor[IP]

  var createdIPs: HashSet[IP] = new HashSet[IP]()

  import sqlContext.createSchemaRDD

  // initialize a SparkPlan that is a sequential scan over a small amount of data
  val smallRDD1: RDD[IP] = sparkContext.parallelize((1 to 100).map(i => {
    val ip: IP = IP(random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256))
    createdIPs += ip
    ip
  }), 1)
  val smallScan1: SparkPlan = PhysicalRDD(IPAttributes, smallRDD1)

  test ("simple dns join") {
    val outputRDD = GeneralDNSJoin(IPAttributes, IPAttributes, smallScan1, smallScan1).execute()

    val result = outputRDD.collect
    assert(result.length == 100)

    result.foreach(x => {
      val ip = IP(x.getString(0))
      assert(createdIPs contains ip)
      createdIPs remove ip
    })
  }
}
