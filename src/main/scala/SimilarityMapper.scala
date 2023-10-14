package com.lsc

import com.lsc.GraphSharder.loadGraphFromHDFS
import org.apache.hadoop.io.{LongWritable, Text}
import org.apache.hadoop.mapreduce.Mapper
import org.slf4j.LoggerFactory

class SimilarityMapper extends Mapper[LongWritable, Text, Text, Text] {

  // Define the basic types for clarity
  private type Node = Int
  private type Graph = Map[Node, Set[Node]]
  private val simRank = new SimRank()
  
  // Logger for capturing logs
  private val logger = LoggerFactory.getLogger(this.getClass)
  
  // Member variables for the original and perturbed graphs
  var originalGraph: Graph = _
  var perturbedGraph: Graph = _

  /** Set up the Mapper by loading graphs from HDFS.
    *
    * @param context The Mapper's context.
    */
  override def setup(
      context: Mapper[LongWritable, Text, Text, Text]#Context
  ): Unit = {
    val cachedFiles = context.getCacheFiles.map(_.getPath)

    // Logging the cached files
    logger.info(s"Cached files: ${cachedFiles.mkString(", ")}")
    if (cachedFiles.length < 2) {
      logger.error("Not enough cached files available.")
      return
    }

    try {
      originalGraph = loadGraphFromHDFS(cachedFiles(0))
      perturbedGraph = loadGraphFromHDFS(cachedFiles(1))
      logger.info("Successfully loaded original and perturbed graphs.")
    } catch {
      case e: Exception =>
        logger.error(s"Error loading graphs: ${e.getMessage}", e)
    }
  }

  /** Process the input key-value pair, compute similarity, and write the output.
    *
    * @param key The input key.
    * @param value The input value (pair of nodes).
    * @param context The Mapper's context.
    */
  override def map(
      key: LongWritable,
      value: Text,
      context: Mapper[LongWritable, Text, Text, Text]#Context
  ): Unit = {
    logger.info(s"Processing value: $value")

    // Split the nodes from the input value
    val nodes = value.toString.split(",")
    if (nodes.length == 2) {
      val node1 = nodes(0).trim.toInt
      val node2 = nodes(1).trim.toInt

      // Compute similarity using the SimRank class's jaccardSimilarity method
      val similarity = simRank.jaccardSimilarity(
        originalGraph,
        node1,
        perturbedGraph,
        node2,
        depth = ConfigurationLoader.getSimilarityDepth
      )

      logger.debug(
        s"Computed similarity for nodes ($node1, $node2) is: $similarity"
      )

      // if the similarity does not pass the threshold then don't write it to the output
      if (similarity >= ConfigurationLoader.getSimilarityThreshold) {
        // Write the output as <node1, node2> -> similarity
        context.write(
          new Text(s"$node1, $node2"),
          new Text(similarity.toString)
        )
        logger.info(
          s"Writing similarity value for nodes ($node1, $node2): $similarity"
        )
      } else {
        logger.warn(
          s"Similarity did not pass the specified threshold for nodes: $value"
        )
      }
    }
  }
}
