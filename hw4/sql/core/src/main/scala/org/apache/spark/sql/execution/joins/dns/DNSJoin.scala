package org.apache.spark.sql.execution.joins.dns

import java.util.{HashMap => JavaHashMap, ArrayList => JavaArrayList}
import java.util.concurrent.ConcurrentHashMap

import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.{JoinedRow, Projection, Expression}
import org.apache.spark.sql.execution.SparkPlan

/**
 * In this join, we are going to implement an algorithm similar to symmetric hash join.
 * However, instead of being provided with two input relations, we are instead going to
 * be using a single dataset and obtaining the other data remotely -- in this case by
 * asynchronous HTTP requests.
 *
 * The dataset that we are going to focus on reverse DNS, latitude-longitude lookups.
 * That is, given an IP address, we are going to try to obtain the geographical
 * location of that IP address. For this end, we are going to use a service called
 * telize.com, the owner of which has graciously allowed us to bang on his system.
 *
 * For that end, we have provided a simple library that makes asynchronously makes
 * requests to telize.com and handles the responses for you. You should read the
 * documentation and method signatures in DNSLookup.scala closely before jumping into
 * implementing this.
 *
 * The algorithm will work as follows:
 * We are going to be a bounded request buffer -- that is, we can only have a certain number
 * of unanswered requests at a certain time. When we initialize our join algorithm, we
 * start out by filling up our request buffer. On a call to next(), you should take all
 * the responses we have received so far and materialize the results of the join with those
 * responses and return those responses, until you run out of them. You then materialize
 * the next batch of joined responses until there are no more input tuples, there are no
 * outstanding requests, and there are no remaining materialized rows.
 *
 */
 
trait DNSJoin {
  self: SparkPlan =>

  val leftKeys: Seq[Expression]
  val left: SparkPlan

  override def output = left.output

  @transient protected lazy val leftKeyGenerator: Projection =
    newProjection(leftKeys, left.output)

  // How many outstanding requests we can have at once.
  val requestBufferSize: Int = 300

  /**
   * The main logic for DNS join. You do not need to implement anything outside of this method.
   * This method takes in an input iterator of IP addresses and returns a joined row with the location
   * data for each IP address.
   *
   * If you find the method definitions provided to be counter-intuitive or constraining, feel free to change them.
   * However, note that if you do:
   *  1. we will have a harder time helping you debug your code.
   *  2. Iterators must implement next and hasNext. If you do not implement those two methods, your code will not compile.
   *
   * **NOTE**: You should return JoinedRows, which take two input rows and returns the concatenation of them.
   * e.g., `new JoinedRow(row1, row2)`
   *
   * @param input the input iterator
   * @return the result of the join
   */
  def hashJoin(input: Iterator[Row]): Iterator[Row] = {
    new Iterator[Row] {
      val output: JavaArrayList[JoinedRow] = new JavaArrayList[JoinedRow]
      val count = new ConcurrentHashMap[String, Int]
      val local_cache = new ConcurrentHashMap[String, JoinedRow]
      val requests = new ConcurrentHashMap[Int, Row]
      val responses = new ConcurrentHashMap[Int, Row]
      var req_num = 0
      for ( i <- 0 to requestBufferSize){
        if(input.hasNext){
          var row : Row = input.next()
          val new_ip = leftKeyGenerator(row).getString(0)
          if(count.containsKey(new_ip)){
            val old_count = count.get(new_ip)
            count.replace(new_ip, old_count+1)
          }else{
            requests.put(req_num, row)
            DNSLookup.lookup(req_num, new_ip, responses, requests)
            count.put(new_ip, 1)
            req_num = req_num+1
          }
        }
      } 

      
      
      /**
       * This method returns the next joined tuple.
       *
       * *** THIS MUST BE IMPLEMENTED FOR THE ITERATOR TRAIT ***
       */
      override def next() = {
        output.remove(0)
      }

      /**
       * This method returns whether or not this iterator has any data left to return.
       *
       * *** THIS MUST BE IMPLEMENTED FOR THE ITERATOR TRAIT ***
       */
      override def hasNext() = {
        if (!output.isEmpty()){
          true
        } else { 
          while(output.isEmpty() && !requests.isEmpty){
            var current = responses.keys()
            while(current.hasMoreElements()){
              var key = current.nextElement()
              if (requests.containsKey(key)){
                var matched_row = responses.get(key)
                var matched_request = requests.get(key)
                var joined_row = new JoinedRow(matched_request, matched_row)
                var ip = leftKeyGenerator(matched_request).getString(0)
                local_cache.put(ip, joined_row)
                requests.remove(key)
                for (j <- 1 to count.get(ip)){
                  output.add(joined_row)
                  //println(1)
                  //println(output)
                }
                count.remove(ip)
                var found = false
                while(input.hasNext && !found){
                  val new_input = input.next()
                  val new_ip = leftKeyGenerator(new_input).getString(0)
                  if(local_cache.containsKey(new_ip)){
                    output.add(local_cache.get(new_ip))
                  } else{
                    if(count.containsKey(new_ip)){
                      val old_count = count.get(new_ip)
                      count.replace(new_ip, old_count+1)
                    }else{
                      makeRequest(new_input)
                      found = true
                    }
                  }
                }
              }
            }
          }
          !output.isEmpty()
        }
      }


      /**
       * This method takes the next element in the input iterator and makes an asynchronous request for it.
       */
      private def makeRequest(in: Row) = {
        // IMPLEMENT ME
        val new_ip = leftKeyGenerator(in).getString(0)
        requests.put(req_num, in)
        DNSLookup.lookup(req_num, new_ip, responses, requests)
        count.put(new_ip, 1)
        req_num = req_num+1
      }
    }
  }
}