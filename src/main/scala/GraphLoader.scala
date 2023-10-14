package com.lsc

import org.slf4j.LoggerFactory
import scala.io.Source
import scala.util.matching.Regex
import java.io.{FileNotFoundException, IOException}

object GraphLoader {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private type Node = Int
  private type Graph = Map[Node, Set[Node]]
  private val nodeEdgePattern: Regex = """"(\d+)"\s->\s"(\d+)"""".r

  def loadGraph(filePath: String): Graph = {

    val methodName = "loadGraph"
    logger.info(s"$methodName: Initiating the loading process from $filePath")

    // Open the file once and keep the reference to close it later.
    val source = Source.fromFile(filePath)

    try {
      logger.info(s"$methodName: Opening file $filePath")

      val lines = source.getLines().toList
      logger.info(s"$methodName: Successfully read ${lines.size} lines from $filePath")

      val graph = lines.flatMap { line =>
        nodeEdgePattern.findFirstMatchIn(line).map { m =>
          val node1 = m.group(1).toInt // Convert to Int
          val node2 = m.group(2).toInt // Convert to Int
          logger.debug(s"$methodName: Processing a line: $node1 -> $node2")
          node1 -> node2
        }
      }.groupBy(_._1).view.mapValues(_.map(_._2).toSet).toMap

      logger.info(s"$methodName: Successfully loaded graph from $filePath containing ${graph.size} nodes")

      graph // Return the graph.
    } catch {
      case e: FileNotFoundException =>
        logger.error(s"$methodName: File not found: $filePath", e)
        Map.empty[Node, Set[Node]]
      case e: IOException =>
        logger.error(s"$methodName: Error reading from file: $filePath", e)
        Map.empty[Node, Set[Node]]
      case e: Exception =>
        logger.error(s"$methodName: Unexpected error loading graph from $filePath", e)
        Map.empty[Node, Set[Node]]
    } finally {
      logger.info(s"$methodName: Closing the file $filePath")
      source.close()
    }
  }
}
