package org.apache.spark.sql.execution.joins.dns

import org.apache.spark.sql.catalyst.expressions.Expression
import org.apache.spark.sql.catalyst.plans.physical.{ClusteredDistribution, Partitioning}
import org.apache.spark.sql.execution.{BinaryNode, SparkPlan}

/**
 * This is just Spark-magic to create this operator.
 * @param leftKeys
 * @param rightKeys
 * @param left
 * @param right
 */
case class GeneralSymmetricHashJoin(
    leftKeys: Seq[Expression],
    rightKeys: Seq[Expression],
    left: SparkPlan,
    right: SparkPlan)
  extends BinaryNode with SymmetricHashJoin {

  override def outputPartitioning: Partitioning = left.outputPartitioning

  override def execute() = {
    left.execute().zipPartitions(right.execute()) { (leftIter, rightIter) => {
        symmetricHashJoin(leftIter, rightIter)
      }
    }
  }
}
