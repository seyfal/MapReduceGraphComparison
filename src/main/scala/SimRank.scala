class SimRank {
  
  type Node = Int
  type Graph = Map[Node, Set[Node]]

  private def extractLocalSubgraph(graph: Graph, node: Node, depth: Int): Graph = {
    def traverse(node: Node, depth: Int, visited: Set[Node], subgraph: Graph): Graph = {
      if (depth < 0 || visited.contains(node)) subgraph
      else {
        val neighbors = graph.getOrElse(node, Set())
        val updatedVisited = visited + node
        val updatedSubgraph = subgraph + (node -> neighbors)
        neighbors.foldLeft(updatedSubgraph) { (accSubgraph, neighbor) =>
          traverse(neighbor, depth - 1, updatedVisited, accSubgraph)
        }
      }
    }

    traverse(node, depth, Set(), Map())
  }

  def jaccardSimilarity(graph1: Graph, node1: Node, graph2: Graph, node2: Node, depth: Int): Double = {
    val subgraph1 = extractLocalSubgraph(graph1, node1, depth)
    val subgraph2 = extractLocalSubgraph(graph2, node2, depth)

    val nodes1 = subgraph1.keySet
    val nodes2 = subgraph2.keySet

    val intersectionSize = nodes1.intersect(nodes2).size.toDouble
    val unionSize = nodes1.union(nodes2).size.toDouble

    if (unionSize == 0) 1.0 // Both subgraphs are empty
    else intersectionSize / unionSize
  }
  
}
