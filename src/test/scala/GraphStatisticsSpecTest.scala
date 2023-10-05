import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GraphStatisticsSpecTest extends AnyFlatSpec with Matchers {

  "classifyNodes" should "classify nodes correctly based on golden set" in {
    // Setup
    val outputData = Map(
      (1, 1) -> 1.0,
      (2, 2) -> 0.8
    )

    val goldenSet = Map(
      "Nodes" -> Map(
        "Modified" -> List(2),
        "Removed" -> List(3),
        "Added" -> Map(4 -> 5, 5 -> 6)
      )
    )

    // Call method
    val (sameNodes, modifiedNodes, actualAdded, actualRemoved) = GraphStatistics.classifyNodes(outputData, goldenSet)

    // Assertions
    sameNodes shouldBe Set(1)
    modifiedNodes shouldBe Set(2)
    actualAdded shouldBe Set(4)
    actualRemoved shouldBe Set(3)
  }

  // Add other tests as required
}
