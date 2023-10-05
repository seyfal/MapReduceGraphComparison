import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GraphStatisticsTest extends AnyFlatSpec with Matchers {

  val sampleGoldenSet = Map(
    "Nodes" -> Map(
      "Modified" -> List(12, 61, 48, 21, 67, 91),
      "Removed" -> List(42, 14, 93),
      "Added" -> Map(
        20 -> 109,
        35 -> 108,
        28 -> 107,
        39 -> 106,
        47 -> 105,
        11 -> 104,
        74 -> 103,
        92 -> 102,
        84 -> 101
      )
    )
  )

  "classifyNodes" should "extract nodesData correctly" in {
    val nodesData = sampleGoldenSet.getOrElse("Nodes", Map.empty[String, Any])
    nodesData shouldBe Map(
      "Modified" -> List(12, 61, 48, 21, 67, 91),
      "Removed" -> List(42, 14, 93),
      "Added" -> Map(
        20 -> 109,
        35 -> 108,
        28 -> 107,
        39 -> 106,
        47 -> 105,
        11 -> 104,
        74 -> 103,
        92 -> 102,
        84 -> 101
      )
    )
  }

  // Similarly for actualAdded, actualRemoved, and actualModified

  it should "extract actualAdded correctly" in {
    val nodesData = sampleGoldenSet("Nodes")
    val actualAdded = nodesData.get("Added") match {
      case Some(data: Map[_, _]) => data.keys.map(_.toString.toInt).toSet
      case _ => Set.empty[Int]
    }
    actualAdded shouldBe Set(20, 35, 28, 39, 47, 11, 74, 92, 84)
  }

  it should "extract actualRemoved correctly" in {
    val nodesData = sampleGoldenSet("Nodes")
    val actualRemoved = nodesData.get("Removed") match {
      case Some(data: List[_]) => data.map(_.toString.toInt).toSet
      case _ => Set.empty[Int]
    }
    actualRemoved shouldBe Set(42, 14, 93)
  }

  it should "extract actualModified correctly" in {
    val nodesData = sampleGoldenSet("Nodes")
    val actualModified = nodesData.get("Modified") match {
      case Some(data: List[_]) => data.map(_.toString.toInt).toSet
      case _ => Set.empty[Int]
    }
    actualModified shouldBe Set(12, 61, 48, 21, 67, 91)
  }
}
