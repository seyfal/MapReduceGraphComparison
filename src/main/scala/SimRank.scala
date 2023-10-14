package com.lsc

import org.slf4j.LoggerFactory

class SimRank {

  type Node = Int
  type Graph = Map[Node, Set[Node]]
  private val logger = LoggerFactory.getLogger(getClass)

  /** Compute Jaccard similarity between subgraphs of two nodes from two graphs.
    * Jaccard similarity is the size of the intersection divided by the size of the union of the two sets.
    *
    * @param graph1 The first graph.
    * @param node1 The node from the first graph.
    * @param graph2 The second graph.
    * @param node2 The node from the second graph.
    * @param depth The depth to consider for local subgraph extraction.
    * @return The Jaccard similarity as a Double.
    */
  def jaccardSimilarity(
      graph1: Graph,
      node1: Node,
      graph2: Graph,
      node2: Node,
      depth: Int
  ): Double = {
    logger.trace(
      s"Starting computation of Jaccard similarity for nodes: $node1 and $node2 with depth: $depth"
    )

    // Extract local subgraphs centered around the given nodes up to the specified depth.
    val subgraph1 = extractLocalSubgraph(graph1, node1, depth)
    val subgraph2 = extractLocalSubgraph(graph2, node2, depth)

    val nodes1 = subgraph1.keySet
    val nodes2 = subgraph2.keySet

    // Compute the sizes of the intersection and union of the node sets from the two subgraphs.
    val intersectionSize = nodes1.intersect(nodes2).size.toDouble
    val unionSize = nodes1.union(nodes2).size.toDouble

    // Compute Jaccard similarity.
    val similarity = if (unionSize == 0) 1.0 else intersectionSize / unionSize

    logger.info(
      s"Computed Jaccard similarity for nodes: $node1 and $node2 is $similarity"
    )
    similarity
  }

  /** Extract a local subgraph centered on a given node up to a certain depth.
    * Uses a recursive depth-first search to traverse the graph.
    *
    * @param graph The original graph.
    * @param node The center node.
    * @param depth The depth for subgraph extraction.
    * @return A subgraph as a Map[Node, Set[Node]].
    */
  private def extractLocalSubgraph(
      graph: Graph,
      node: Node,
      depth: Int
  ): Graph = {
    logger.trace(
      s"Starting extraction of local subgraph for node: $node with depth: $depth"
    )

    // Recursive function to traverse the graph and build the subgraph.
    def traverse(
        node: Node,
        depth: Int,
        visited: Set[Node],
        subgraph: Graph
    ): Graph = {
      if (depth < 0 || visited.contains(node)) {
        logger.trace(
          s"Reached maximum depth or visited node: $node again. Stopping traversal."
        )
        subgraph
      } else {
        val neighbors = graph.getOrElse(node, Set())
        logger.debug(s"Found neighbors for node: $node -> $neighbors")

        val updatedVisited = visited + node
        val updatedSubgraph = subgraph + (node -> neighbors)

        // Recursively traverse neighbors.
        neighbors.foldLeft(updatedSubgraph) { (accSubgraph, neighbor) =>
          traverse(neighbor, depth - 1, updatedVisited, accSubgraph)
        }
      }
    }

    val result = traverse(node, depth, Set(), Map())
    logger.debug(
      s"Completed extraction of local subgraph for node: $node. Extracted subgraph: $result"
    )
    result
  }
}
