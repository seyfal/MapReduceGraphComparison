//import org.scalatest.flatspec.AnyFlatSpec
//import org.scalatest.matchers.should.Matchers
//
//class MapperSpec extends AnyFlatSpec with Matchers {
//
//  type Graph = Map[Node, Set[Node]]
//  type Node = String // Or whatever type you are using
//
//  "extractLocalSubgraph" should "extract correct local subgraph" in {
//    val mapper = new Mapper
//    val graph: Graph = Map(
//      1 -> Set(2, 3),
//      2 -> Set(1, 3),
//      3 -> Set(1, 2)
//    )
//    val node = 1
//    val depth = 1
//    val result = mapper.extractLocalSubgraph(graph, node, depth)
//    result shouldBe Map(1 -> Set(2, 3), 2 -> Set(1, 3), 3 -> Set(1, 2))
//  }
//
//  "jaccardSimilarity" should "return correct similarity for non-empty graphs" in {
//    val mapper = new Mapper
//    val graph1: Graph = Map(1 -> Set(2), 2 -> Set(1))
//    val graph2: Graph = Map(1 -> Set(2), 2 -> Set(1))
//    val result = mapper.jaccardSimilarity(graph1, 1, graph2, 1, 1)
//    result shouldBe 1.0
//  }
//
//  it should "return 1.0 for empty graphs" in {
//    val mapper = new Mapper
//    val graph1: Graph = Map()
//    val graph2: Graph = Map()
//    val result = mapper.jaccardSimilarity(graph1, 1, graph2, 1, 1)
//    result shouldBe 1.0
//  }
//
//  // Add more tests as needed.
//}
