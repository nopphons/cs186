package org.apache.spark.sql.execution

import org.apache.spark.sql.catalyst.ScalaReflection
import org.apache.spark.sql.catalyst.expressions.{Expression, Attribute, ScalaUdf, Row}
import org.apache.spark.sql.catalyst.types.IntegerType
import org.scalatest.FunSuite

import scala.collection.mutable.ArraySeq
import scala.util.Random

case class Student(sid: Int, gpa: Float)

class CS186UtilsSuite extends FunSuite {
  val numberGenerator: Random = new Random()

  val studentAttributes: Seq[Attribute] =  ScalaReflection.attributesFor[Student]

  // TESTS FOR TASK #3
  /* NOTE: This test is not a guarantee that your caching iterator is completely correct.
     However, if your caching iterator is correct, then you should be passing this test. */
  test("caching iterator") {
    val list: ArraySeq[Row] = new ArraySeq[Row](1000)

    for (i <- 0 to 999) {
      list(i) = (Row(numberGenerator.nextInt(10000), numberGenerator.nextFloat()))
    }


    val udf: ScalaUdf = new ScalaUdf((sid: Int) => sid + 1, IntegerType, Seq(studentAttributes(0)))

    val result: Iterator[Row] = CachingIteratorGenerator(studentAttributes, udf, Seq(studentAttributes(1)), Seq(), studentAttributes)(list.iterator)

    assert(result.hasNext)

    result.foreach((x: Row) => {
      val inputRow: Row = Row(x.getInt(1) - 1, x.getFloat(0))
      assert(list.contains(inputRow))
    })
  }

  test("sequence with 1 UDF") {
    val udf: ScalaUdf = new ScalaUdf((i: Int) => i + 1, IntegerType, Seq(studentAttributes(0)))
    val attributes: Seq[Expression] = Seq() ++ studentAttributes ++ Seq(udf)

    assert(CS186Utils.getUdfFromExpressions(attributes) == udf)
  }
}