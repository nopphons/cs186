package org.apache.spark.sql.execution.joins.dns

import java.io.{InputStreamReader, BufferedReader}
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

import org.apache.spark.sql.Row

import scala.concurrent._
import ExecutionContext.Implicits.global

object DNSLookup {
  val BASE_IP_URL = "http://telize.com/geoip/"

  /**
    * This method takes in a request number, an IP address, and two concurrent hash maps (because we are multi threading
    * when using Futures). It makes the request. If it succeeds, it inserts the resulting Row into the responses. Else,
    * it removes the corresponding row from requests.
    *
    * @param requestNumber The request number in order to match requests and responses
    * @param IP the ip we are doing the look up for
    * @param responses the data structure used to update responses
    * @param requests the data structure used to keep track of requests
    */
  def lookup(requestNumber: Int, IP: String, responses: ConcurrentHashMap[Int, Row], requests: ConcurrentHashMap[Int, Row]) = {
    val responseFuture: Future[String] = future {
      val url = new URL(BASE_IP_URL + IP)
      val conn = url.openConnection()

      val rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      var result: String = ""
      var line: Option[String] = null
      line = Option(rd.readLine())
      while (! line.isEmpty && !line.equals(None)) {
        result += line.get;

        line = Option(rd.readLine())
      }

      result
    }

    responseFuture onSuccess {
      case response: String => {
        val row: Row = convertJson(response)
        responses put(requestNumber, row)
      }
    }

    responseFuture onFailure {
      case response => {
        requests remove(requestNumber)
      }
    }
  }

  def convertJson(json: String): Row = {
    val response: String = json

    val map: Map[String, String] = toMap[String](response)
    val values = Seq(if (map.get("latitude").isEmpty) None else map.get("latitude").get,
      if (map.get("longitude").isEmpty) None else map.get("longitude").get,
      if (map.get("city").isEmpty) None else map.get("city").get,
      if (map.get("region").isEmpty) None else map.get("region").get,
      if (map.get("country_code").isEmpty) None else map.get("country_code").get)

    Row.fromSeq(values)
  }

  /**
   * Credit for this code snippet goes to this blog: https://coderwall.com/p/o--apg/easy-json-un-marshalling-in-scala-with-jackson
   */

  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  def toMap[V](json:String)(implicit m: Manifest[V]) = fromJson[Map[String,V]](json)

  def fromJson[T](json: String)(implicit m : Manifest[T]): T = {
    mapper.readValue[T](json)
  }
}
