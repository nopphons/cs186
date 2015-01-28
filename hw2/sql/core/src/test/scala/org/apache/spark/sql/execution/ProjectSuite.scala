package org.apache.spark.sql.execution

import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.catalyst.ScalaReflection
import org.apache.spark.sql.catalyst.expressions.ScalaUdf
import org.apache.spark.sql.catalyst.types.IntegerType
import org.apache.spark.sql.catalyst.expressions.{Attribute, Row}
import org.scalatest.FunSuite
import org.apache.spark.sql.test.TestSQLContext._

import scala.collection.immutable.HashSet

case class Record(i: Int)

class ProjectSuite extends FunSuite {
  // initialize Spark magic stuff that we don't need to care about
  val sqlContext = new SQLContext(sparkContext)
  val recordAttributes: Seq[Attribute] = ScalaReflection.attributesFor[Record]

  // define a simple ScalaUdf that adds one to each integer
  val udf: ScalaUdf = ScalaUdf((i: Int) => i + 1, IntegerType, recordAttributes)

  // initialize a SparkPlan that is a sequential scan over a small amount of data
  val smallRDD = sparkContext.parallelize((1 to 100).map(i => Record(i)), 1)
  val smallScan: SparkPlan = PhysicalRDD(recordAttributes, smallRDD)

  // initialize a SparkPlan that is a sequential scan over a large amount of data
  val largeRDD = sparkContext.parallelize((1 to 10000).map(i => Record(i)), 1)
  val largeScan: SparkPlan = PhysicalRDD(recordAttributes, largeRDD)

  // TESTS FOR TASK #4
  // functionality test for the PartitionProject Operator
  test("PartitionProject") {
    val outputRDD = PartitionProject(Seq(udf), largeScan).execute()
    var seenValues: HashSet[Row] = new HashSet[Row]()

    outputRDD.collect().foreach(x => seenValues = seenValues + x)

    (2 to 10001).foreach(x => assert(seenValues.contains(Row(x))))
  }
}
