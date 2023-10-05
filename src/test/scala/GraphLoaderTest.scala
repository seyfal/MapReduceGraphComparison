import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import GraphLoader.loadGraph

import java.nio.file.{Files, Paths}
import scala.jdk.CollectionConverters.*

class GraphLoaderSpec extends AnyWordSpec with Matchers {

  "GraphLoader" when {
    "loading a graph" should {
      "load a well-formed graph file correctly" in {
        val filePath = Files.createTempFile("graph", "txt")
        Files.write(filePath, List("\"1\" -> \"2\"", "\"2\" -> \"3\"").asJava)

        val graph = loadGraph(filePath.toString)
        graph shouldEqual Map("1" -> Set("2"), "2" -> Set("3"))

        Files.delete(filePath)
      }

      "return an empty graph for an empty file" in {
        val filePath = Files.createTempFile("emptyGraph", "txt")

        val graph = loadGraph(filePath.toString)
        graph shouldBe empty

        Files.delete(filePath)
      }

      "return an empty graph for a non-existent file" in {
        val graph = loadGraph("nonExistentFile.txt")
        graph shouldBe empty
      }

      "ignore malformed lines" in {
        val filePath = Files.createTempFile("malformedGraph", "txt")
        Files.write(filePath, List("\"1\" -> \"2\"", "malformed line", "\"2\" -> \"3\"").asJava)

        val graph = loadGraph(filePath.toString)
        graph shouldEqual Map("1" -> Set("2"), "2" -> Set("3"))

        Files.delete(filePath)
      }
    }
  }
}
