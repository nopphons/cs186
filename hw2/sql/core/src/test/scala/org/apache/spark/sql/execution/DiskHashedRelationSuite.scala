package org.apache.spark.sql.execution

import org.apache.spark.sql.catalyst.expressions.{Projection, Row}
import org.scalatest.FunSuite

import scala.collection.mutable.ArraySeq

class DiskHashedRelationSuite extends FunSuite {

  private val keyGenerator = new Projection {
    override def apply(row: Row): Row = row
  }

  // TESTS FOR TASK #2
  test("values are in correct partition") {
    val data: Array[Row] = (0 to 100).map(i => Row(i)).toArray
    val hashedRelation: DiskHashedRelation = DiskHashedRelation(data.iterator, keyGenerator, 3, 64000)
    var count: Int = 0

    for (partition <- hashedRelation.getIterator()) {
      for (row <- partition.getData()) {
        assert(row.hashCode() % 3 == count)
      }
      count += 1
    }
  }

  test ("empty input") {
    val data: ArraySeq[Row] = new ArraySeq[Row](0)
    val hashedRelation: DiskHashedRelation = DiskHashedRelation(data.iterator, keyGenerator)

    for (partition <- hashedRelation.getIterator()) {
      assert(!partition.getData.hasNext)
    }

    hashedRelation.closeAllPartitions()
  }
}