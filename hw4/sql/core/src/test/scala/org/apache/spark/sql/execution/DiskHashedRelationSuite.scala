package org.apache.spark.sql.execution

import org.apache.spark.SparkException
import org.apache.spark.sql.catalyst.expressions.{Projection, Row}
import org.scalatest.FunSuite

import scala.collection.immutable.HashSet
import scala.collection.mutable.ArraySeq

class DiskHashedRelationSuite extends FunSuite {

  private val keyGenerator = new Projection {
    override def apply(row: Row): Row = row
  }

  test("values are in correct partition") {
    val data: Array[Row] = (0 to 100).map(i => Row(i)).toArray
    val hashedRelation = DiskHashedRelation(data.iterator, keyGenerator, 3, 64000)
    var count: Int = 0

    for (partition <- hashedRelation.getIterator()) {
      for (row <- partition.getData()) {
        assert(row.hashCode() % 3 == count)
      }
      count += 1
    }
  }

  // this tests the case in which the writeThreshold is low enough that ever write will cause the partition to spill
  test("one row per write") {
    val data: Array[Row] = (0 to 3).map(i => Row(i)).toArray
    val numPartitions = 2
    val writeThreshold = 2
    val hashedRelation = DiskHashedRelation(data.iterator, keyGenerator, numPartitions, writeThreshold)

    var seenValues: HashSet[Row] = new HashSet[Row]()

    for (partition <- hashedRelation.getIterator()) {
      for (row <- partition.getData()) {
        seenValues += row
      }
    }

    (0 to 3).foreach(x => assert(seenValues.contains(Row(x))))
    hashedRelation.closeAllPartitions()
  }

  // this tests the case where we have multiple insertions (I think ~150?) before we spill
  test ("larger input sizes") {
    val data: Array[Row] = (0 to 10000).toArray.map(x => Row(x))
    var seenValues: HashSet[Row] = new HashSet[Row]()
    var count: Int = 0

    val hashedRelation = DiskHashedRelation(data.iterator, keyGenerator)

    for (partition <- hashedRelation.getIterator()) {
      for (row <- partition.getData()) {
        seenValues = seenValues + row
      }
      count += 1
    }

    (0 to 10000).foreach(x => assert(seenValues.contains(Row(x))))
    hashedRelation.closeAllPartitions()
  }

  // this tests the case where we hash no data; this isn't really a "useful" test (why would we
  // ever hash no data?) but is here for software engineering thoroughness
  test ("empty input") {
    val data: ArraySeq[Row] = new ArraySeq[Row](0)
    val hashedRelation = DiskHashedRelation(data.iterator, keyGenerator)

    for (partition <- hashedRelation.getIterator()) {
      assert(!partition.getData.hasNext)
    }

    hashedRelation.closeAllPartitions()
  }

  test ("some empty partitions") {
    val data: Array[Row] = (0 to 62).toArray.map(x => Row(x))
    var seenValues: HashSet[Row] = new HashSet[Row]()
    var count: Int = 0
    val hashedRelation = DiskHashedRelation(data.iterator, keyGenerator)

    for (partition <- hashedRelation.getIterator()) {
      for (row <- partition.getData()) {
        seenValues += row
      }
    }

    assert(seenValues.size == 63)
    (0 to 62).foreach(x => assert(seenValues.contains(Row(x))))
  }
}