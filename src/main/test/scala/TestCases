package com.lsc

import org.scalatest.funsuite.AnyFunSuite

class TestCases extends AnyFunSuite {

  type Node = Int
  type Graph = Map[Node, Set[Node]]
  val simRank = new SimRank()

  test("Jaccard similarity with no overlap") {
    val graphA: Graph = Map(1 -> Set(2), 2 -> Set(3))
    val graphB: Graph = Map(4 -> Set(5), 5 -> Set(6))
    val result = simRank.jaccardSimilarity(graphA, 1, graphB, 4, 2)
    assert(result == 0.0)
  }

  test("Jaccard similarity with partial overlap") {
    val graphA: Graph = Map(1 -> Set(2, 3), 2 -> Set(3), 3 -> Set(1))
    val graphB: Graph = Map(1 -> Set(4), 4 -> Set(3))
    val result = simRank.jaccardSimilarity(graphA, 1, graphB, 1, 2)
    assert(result > 0.0 && result < 1.0)
  }

  test("Jaccard similarity with complete overlap") {
    val graphA: Graph = Map(1 -> Set(2, 3), 2 -> Set(3), 3 -> Set(1))
    val result = simRank.jaccardSimilarity(graphA, 1, graphA, 1, 2)
    assert(result == 1.0)
  }

  test("Extraction of local subgraph with depth") {
    val graph: Graph = Map(1 -> Set(2), 2 -> Set(3, 4), 3 -> Set(4), 4 -> Set(5))
    val expectedSubgraph: Graph = Map(1 -> Set(2), 2 -> Set(3, 4))

    // Using reflection to access the private method
    val method = simRank.getClass.getDeclaredMethod("extractLocalSubgraph", classOf[Graph], classOf[Node], classOf[Int])
    method.setAccessible(true)
    val subgraph = method.invoke(simRank, graph, 1: Node, 1: Int).asInstanceOf[Graph]

    assert(subgraph == expectedSubgraph)
  }

  test("Extraction of local subgraph for non-existing node") {
    val graph: Graph = Map(1 -> Set(2))

    // Using reflection to access the private method
    val method = simRank.getClass.getDeclaredMethod("extractLocalSubgraph", classOf[Graph], classOf[Node], classOf[Int])
    method.setAccessible(true)
    val subgraph = method.invoke(simRank, graph, 3: Node, 1: Int).asInstanceOf[Graph]

    assert(subgraph == Map(3 -> Set()))
  }

  // TODO: Add more tests later
}
