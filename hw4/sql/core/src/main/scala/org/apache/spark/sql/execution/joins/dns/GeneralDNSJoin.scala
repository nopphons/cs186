package org.apache.spark.sql.execution.joins.dns

import org.apache.spark.sql.catalyst.expressions.Expression
import org.apache.spark.sql.catalyst.plans.physical.Partitioning
import org.apache.spark.sql.execution.{BinaryNode, SparkPlan}

case class GeneralDNSJoin (
    leftKeys: Seq[Expression],
    rightKeys: Seq[Expression],
    left: SparkPlan,
    right: SparkPlan) extends BinaryNode with DNSJoin {

  override def outputPartitioning: Partitioning = left.outputPartitioning

  override def execute() = {
    left.execute().mapPartitions { leftIter => {
        hashJoin(leftIter)
      }
    }
  }

}
