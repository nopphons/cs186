package org.apache.spark.sql.execution

import org.apache.spark.SparkException
import org.apache.spark.sql.catalyst.expressions.Row
import org.scalatest.FunSuite

class DiskPartitionSuite extends FunSuite {

  // TESTS FOR TASK #1
  test ("disk partition") {
    val partition: DiskPartition = new DiskPartition("disk partition test", 2000)

    for (i <- 1 to 500) {
      partition.insert(Row(i))
    }

    partition.closeInput()

    val data: Array[Row] = partition.getData.toArray
    (1 to 500).foreach((x: Int) => assert(data.contains(Row(x))))
  }

  test ("close input") {
    val partition: DiskPartition = new DiskPartition("close input test", 1)

    intercept[SparkException] {
      partition.getData()
    }

    partition.closeInput()

    intercept[SparkException] {
      partition.insert(Row(1))
    }
  }
}
