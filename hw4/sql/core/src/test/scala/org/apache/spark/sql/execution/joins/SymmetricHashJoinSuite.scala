package org.apache.spark.sql.execution.joins

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.catalyst.ScalaReflection
import org.apache.spark.sql.catalyst.expressions.{JoinedRow, Row, Attribute}
import org.apache.spark.sql.execution.joins.dns.GeneralSymmetricHashJoin
import org.apache.spark.sql.execution.{Record, ComplicatedRecord, PhysicalRDD, SparkPlan}
import org.apache.spark.sql.test.TestSQLContext._
import org.scalatest.FunSuite

import scala.collection.immutable.HashSet


class SymmetricHashJoinSuite extends FunSuite {
  // initialize Spark magic stuff that we don't need to care about
  val sqlContext = new SQLContext(sparkContext)
  val recordAttributes: Seq[Attribute] = ScalaReflection.attributesFor[Record]
  val complicatedAttributes: Seq[Attribute] = ScalaReflection.attributesFor[ComplicatedRecord]

  import sqlContext.createSchemaRDD

  // initialize a SparkPlan that is a sequential scan over a small amount of data
  val smallRDD1: RDD[Record] = sparkContext.parallelize((1 to 100).map(i => Record(i)), 1)
  val smallScan1: SparkPlan = PhysicalRDD(recordAttributes, smallRDD1)
  val smallRDD2: RDD[Record] = sparkContext.parallelize((51 to 150).map(i => Record(i)), 1)
  val smallScan2: SparkPlan = PhysicalRDD(recordAttributes, smallRDD2)

  // the same as above but complicated
  val complicatedRDD1: RDD[ComplicatedRecord] = sparkContext.parallelize((1 to 100).map(i => ComplicatedRecord(i, i.toString, i*2)), 1)
  val complicatedScan1: SparkPlan = PhysicalRDD(complicatedAttributes, complicatedRDD1)
  val complicatedRDD2: RDD[ComplicatedRecord] = sparkContext.parallelize((51 to 150).map(i => ComplicatedRecord(i, i.toString, i*2)), 1)
  val complicatedScan2: SparkPlan = PhysicalRDD(complicatedAttributes, complicatedRDD2)

 test ("simple join") {
    val outputRDD = GeneralSymmetricHashJoin(recordAttributes, recordAttributes, smallScan1, smallScan2).execute()
    var seenValues: HashSet[Row] = new HashSet[Row]()

    outputRDD.collect().foreach(x => seenValues = seenValues + x)

    (51 to 100).foreach(x => assert(seenValues.contains(new JoinedRow(Row(x), Row(x)))))
  }

  test ("complicated join") {
    val outputRDD = GeneralSymmetricHashJoin(Seq(complicatedAttributes(0)), Seq(complicatedAttributes(0)), complicatedScan1, complicatedScan2).execute()
    var seenValues: HashSet[Row] = new HashSet[Row]()

    outputRDD.collect().foreach(x => seenValues = seenValues + x)

    (51 to 100).foreach(x => assert(seenValues.contains(new JoinedRow(Row(x, x.toString, x*2), Row(x, x.toString, x*2)))))
  }
}
