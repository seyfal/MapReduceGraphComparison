package com.lsc

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import scala.io.Source

/** Utility object for loading the results from the output file stored in HDFS.
  * The results are loaded into a Map data structure.
  */
object OutputFileLoader {

  /** Loads the similarity scores from the specified HDFS path and returns them
    * as a Map with the pair of nodes as keys and their similarity score as the value.
    *
    * @param hdfsPath The path in HDFS where the output file is located.
    * @return A Map with node pairs as keys and similarity scores as values.
    */
  def loadOutputFile(hdfsPath: String): Map[(Int, Int), Double] = {

    // Setting up HDFS configuration
    val conf = new Configuration()
    conf.set("fs.defaultFS", "hdfs://localhost:9000/")
    val fs = FileSystem.get(conf)
    val path = new Path(hdfsPath)

    // Opening the HDFS file for reading
    val stream = fs.open(path)
    val source = Source.createBufferedSource(stream)

    // Parsing the output data into a map
    val outputData = source
      .getLines()
      .map { line =>
        val parts = line.split("\t")
        val nodes = parts(0).split(", ").map(_.trim.toInt)
        val score = parts(1).toDouble

        (nodes(0), nodes(1)) -> score
      }
      .toMap

    source.close()
    outputData
  }
}
