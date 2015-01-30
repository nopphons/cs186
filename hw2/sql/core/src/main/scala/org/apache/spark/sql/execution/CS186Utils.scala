package org.apache.spark.sql.execution

import java.io._
import java.util.{ArrayList => JavaArrayList, HashMap => JavaHashMap}

import org.apache.spark.sql.catalyst.expressions._

object CS186Utils {

  /**
   * Returns a Scala array that contains the bytes representing a Java ArrayList.
   *
   * @param data the Java ArrayList we are converting
   * @return an array of bytes
   */
  def getBytesFromList(data: JavaArrayList[Row]): Array[Byte] = {
    // create a ObjectOutputStream backed by a ByteArrayOutputStream
    val bytes = new ByteArrayOutputStream()
    val out = new ObjectOutputStream(bytes)

    // write the object to the output
    out.writeObject(data)
    out.flush()
    out.close()
    bytes.close()

    // return the byte array
    bytes.toByteArray
  }

  /**
   * Converts an array of bytes into a JavaArrayList of type [[Row]].
   *
   * @param bytes the input byte array
   * @return a [[JavaArrayList]] of Rows
   */
  def getListFromBytes(bytes: Array[Byte]): JavaArrayList[Row] = {
    val result: JavaArrayList[Row] = new JavaArrayList[Row]()
    var temp: JavaArrayList[Row] = null

    // create input streams based on the input bytes
    val bytesIn = new ByteArrayInputStream(bytes)
    var in = new ObjectInputStream(bytesIn)

    try {
      // read each object in and attempt to interpret it as a JavaArrayList[Row]
      while ((temp = in.readObject() match {
        case value: JavaArrayList[Row] => value
        case _: Throwable => throw new RuntimeException(s"Unexpected casting exception while reading from file.")
      }) != null) {
        // if it succeeds, add it to the result
        result.addAll(temp)

        // we need to create a new ObjectInputStream for each new object we read because of Java stream quirks
        in = new ObjectInputStream(bytesIn)
      }
    } catch {
      // ObjectInputStream control flow dictates that an EOFException will be thrown when the file is over -- this is expected
      case e: EOFException => // do nothing
      case other: Throwable => throw other
    }

    result
  }

  /**
   * Reads the next nextChunkSize bytes from the input stream provided. If the previous array read into is availab
   * please provide it so as to avoid allocating new object unless absolutely necessary.
   *
   * @param inStream the input stream we are reading from
   * @param nextChunkSize the number of bytes to read
   * @param previousArray the previous array we read into
   * @return
   */
  def getNextChunkBytes(inStream: InputStream, nextChunkSize: Int, previousArray: Array[Byte] = null): Array[Byte] = {
    var byteArray = previousArray
    if (byteArray == null || byteArray.size != nextChunkSize) {
      byteArray = new Array[Byte](nextChunkSize)
    }

    // Read the bytes in.
    inStream.read(byteArray)

    byteArray
  }

  /**
   * Return a new projection operator.
   *
   * @param expressions
   * @param inputSchema
   * @return
   */
  def getNewProjection(
                      expressions: Seq[Expression],
                      inputSchema: Seq[Attribute]) = new InterpretedProjection(expressions, inputSchema)


  /**
   * This function returns the [[ScalaUdf]] from a sequence of expressions. If there is no UDF in the
   * sequence of expressions then it returns null. If there is more than one, it returns the one that is
   * sequentially last.
   *
   * @param expressions
   * @return
   */
  def getUdfFromExpressions(expressions: Seq[Expression]): ScalaUdf = {
    // IMPLEMENT ME
    null
  }

  /**
   * This function takes a sequence of expressions. If there is no UDF in the sequence of expressions, it does
   * a regular projection operation.
   *
   * If there is a UDF, then it creates a caching iterator that caches the result of the UDF.
   *
   * NOTE: This only works for a single UDF. If there are multiple UDFs, then it will only cache for the last UDF
   * and execute all other UDFs regularly.
   *
   * @param expressions
   * @param inputSchema
   * @return
   */
  def generateCachingIterator(
                                expressions: Seq[Expression],
                                inputSchema: Seq[Attribute]): (Iterator[Row] => Iterator[Row]) = {
    // Get the UDF from the expressions.
    val udf: ScalaUdf = CS186Utils.getUdfFromExpressions(expressions)

    udf match {
      /* If there is no UDF, then do a regular projection operation. Note that this is very similar to Project in
         basicOperators.scala */
      case null => {
        { input =>
          val projection = CS186Utils.getNewProjection(expressions, inputSchema)
          input.map(projection)
        }
      }

      // Otherwise, separate the expressions appropriately and creating a caching iterator.
      case u: ScalaUdf => {
        val udfIndex: Int = expressions.indexOf(u)
        val preUdfExpressions = expressions.slice(0, udfIndex)
        val postUdfExpressions = expressions.slice(udfIndex + 1, expressions.size)

        CachingIteratorGenerator(udf.children, udf, preUdfExpressions, postUdfExpressions, inputSchema)
      }
    }

  }
}


object CachingIteratorGenerator {
  /**
   * This function takes an input iterator and returns an iterator that does in-memory memoization
   * as it evaluates the projection operator over each input row. The result is the concatenation of
   * the projection of the preUdfExpressions, the evaluation of the udf, and the projection of the
   * postUdfExpressions, in that order.
   *
   * The UDF should only be evaluated if the inputs to the UDF have never been seen before.
   *
   * This method only needs to worry about caching for the UDF that is specifically passed in. If
   * there are any other UDFs in the expression lists, then they can and should be evaluated
   * without any caching.
   *
   * @param cacheKeys the keys on which we will cache -- the inputs to the UDF
   * @param udf the udf we are caching for
   * @param preUdfExpressions the expressions that come before the UDF in the projection
   * @param postUdfExpressions the expressions that come after the UDF in the projection
   * @param inputSchema the schema of the rows -- useful for creating projections
   * @return
   */
  def apply(
               cacheKeys: Seq[Expression],
               udf: ScalaUdf,
               preUdfExpressions: Seq[Expression],
               postUdfExpressions: Seq[Expression],
               inputSchema: Seq[Attribute]): (Iterator[Row] => Iterator[Row]) = {

    { input =>
      new Iterator[Row] {
        val udfProject = CS186Utils.getNewProjection(Seq(udf), inputSchema)
        val cacheKeyProjection = CS186Utils.getNewProjection(udf.children, inputSchema)
        val preUdfProjection = CS186Utils.getNewProjection(preUdfExpressions, inputSchema)
        val postUdfProjection = CS186Utils.getNewProjection(postUdfExpressions, inputSchema)
        val cache: JavaHashMap[Row, Row] = new JavaHashMap[Row, Row]()

        def hasNext() = {
          // IMPLEMENT ME
          false
        }

        def next() = {
          // IMPLEMENT ME
          null
        }
      }
    }
  }
}
