package org.apache.spark.sql.execution

import java.io._
import java.nio.charset.Charset
import java.nio.file.{Path, StandardOpenOption, Files, Paths}
import java.util.{ArrayList => JavaArrayList}

import com.google.common.annotations.VisibleForTesting
import org.apache.spark.SparkException
import org.apache.spark.sql.catalyst.expressions.{Projection, Row}
import org.apache.spark.sql.execution.CS186Utils._

import scala.collection.JavaConverters._

/**
 * This trait represents a regular relation that is hash partitioned and spilled to
 * disk.
 */
private[sql] sealed trait DiskHashedRelation {
  /**
   *
   * @return an iterator of the [[DiskPartition]]s that make up this relation.
   */
  def getIterator(): Iterator[DiskPartition]

  /**
   * Close all the partitions for this relation. This should involve deleting the files hashed into.
   */
  def closeAllPartitions()
}

/**
 * A general implementation of [[DiskHashedRelation]].
 *
 * @param partitions the disk partitions that we are going to spill to
 */
private[sql] final class GeneralDiskHashedRelation(partitions: Array[DiskPartition])
    extends DiskHashedRelation with Serializable {

  override def getIterator() = {
    partitions.iterator
  }

  override def closeAllPartitions() = {
    partitions.foreach(p => p.closePartition())
  }
}

protected[sql] class DiskPartition (
                                  filename: String,
                                  writeThreshold: Int) {
  private val path: Path = Files.createTempFile("", filename)
  private val data: JavaArrayList[Row] = new JavaArrayList[Row]
  private val outStream: OutputStream = Files.newOutputStream(path)
  private val inStream: InputStream = Files.newInputStream(path)
  private val chunkSizes: JavaArrayList[Int] = new JavaArrayList[Int]()
  private var writtenToDisk: Boolean = false
  private var inputClosed: Boolean = false

  /**
   * This method inserts a new row into this particular partition. If the size of the partition
   * exceeds the writeThreshold, the partition is spilled to disk.
   *
   * @param row the [[Row]] we are adding
   */
  def insert(row: Row) = {
    if (inputClosed) {
      throw new SparkException("Cannot insert to this partition after closing the input!")
    }

    data.add(row)

    if (measurePartitionSize() > writeThreshold) {
      spillPartitionToDisk()
      data.removeAll(data)
    }
  }

  /**
   * This method converts the data to a byte array and returns the size of the byte array
   * as an estimation of the size of the partition.
   *
   * @return the estimated size of the data
   */
  private[this] def measurePartitionSize(): Int = {
    getBytesFromList(data).size
  }

  /**
   * Uses the [[Files]] API to write a byte array representing data to a file.
   */
  private[this] def spillPartitionToDisk() = {
    val bytes: Array[Byte] = getBytesFromList(data)

    // This array list stores the sizes of chunks written in order to read them back correctly.
    chunkSizes.add(bytes.size)

    Files.write(path, bytes, StandardOpenOption.APPEND)
    writtenToDisk = true
  }

  /**
   * If this partition has been closed, this method returns an ArrayList with all the
   * data that was written to disk by this partition.
   *
   * @return the [[JavaArrayList]] of the data
   */
  def getData(): Iterator[Row] = {
    if (!inputClosed) {
      throw new SparkException("Should not be reading from file before closing input. Bad things will happen!")
    }

    new Iterator[Row] {
      var currentIterator: Iterator[Row] = data.iterator.asScala
      val chunkSizeIterator: Iterator[Int] = chunkSizes.iterator().asScala
      var byteArray: Array[Byte] = null

      override def next() = {
        currentIterator.next()
      }

      override def hasNext() = {
        (currentIterator != null && currentIterator.hasNext) || (chunkSizeIterator.hasNext && fetchNextChunk())
      }

      /**
       * Fetches the next chunk of the file and updates the iterator. Should return true
       * unless the iterator is empty.
       *
       * @return true unless the iterator is empty.
       */
      private[this] def fetchNextChunk(): Boolean = {
        // Get the size of the next chunk and reallocate the byte array if necessary.
        byteArray = CS186Utils.getNextChunkBytes(inStream, chunkSizeIterator.next, byteArray)

        // Convert the bytes to an ArrayList and return.
        val dataList: JavaArrayList[Row] = getListFromBytes(byteArray)
        currentIterator = dataList.iterator().asScala
        currentIterator.hasNext
      }
    }
  }

  /**
   * Closes this partition, implying that no more data will be written to this partition. If getData()
   * is called without closing the partition, an error will be thrown.
   */
  def closeInput() = {
    // if there is any data that has not yet been written, write it to disk
    if (!this.data.isEmpty) {
      spillPartitionToDisk()
      data.removeAll(data)
    }

    // construct the input stream to read the data we wrote
    outStream.close()
    inputClosed = true
  }


  /**
   * Closes this partition. This closes the input stream and deletes the file backing the partition.
   */
  private[sql] def closePartition() = {
    inStream.close()
    Files.deleteIfExists(path)
  }
}

private[sql] object DiskHashedRelation {

  /**
   * Given an input iterator, partitions each row into one of a number of [[DiskPartition]]s
   * and constructors a [[DiskHashedRelation]].
   *
   * This executes the first phase of external hashing -- using a course-grained hash function
   * to partition the tuples to disk.
   *
   * @param input the input [[Iterator]] of [[Row]]s
   * @param keyGenerator a [[Projection]] that generates the keys for the input
   * @param size the number of [[DiskPartition]]s
   * @param writeThreshold the threshold at which each partition will spill
   * @return the constructed [[DiskHashedRelation]]
   */
  def apply (
                input: Iterator[Row],
                keyGenerator: Projection,
                size: Int = 64,
                writeThreshold: Int = 64000) = {
    val partitions = new Array[DiskPartition](size)
    var currentRow: Row = null
    var hashCode: Int = 0
    var partitionNumber: Int = 0
    var rowKey: Row = null

    // Initialize the disk partitions.
    for (i <- 0 to size - 1) {
      partitions(i) = new DiskPartition("partition-" + i, writeThreshold)
    }

    // For each row, generate a key, get the hash code, choose a partition, and insert it.
    while (input.hasNext) {
      currentRow = input.next()
      rowKey = keyGenerator(currentRow)
      hashCode = rowKey.hashCode()
      partitionNumber = hashCode % size
      partitions(partitionNumber).insert(currentRow)
    }

    // Close the input to all partitions.
    for (partition <- partitions) {
      partition.closeInput()
    }

    new GeneralDiskHashedRelation(partitions)
  }
}