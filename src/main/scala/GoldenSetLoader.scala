import scala.io.Source
import scala.util.matching.Regex

object GoldenSetLoader {

  def main(args: Array[String]): Unit = {
    val (modifiedNodes, removedNodes, addedNodeKeys) = parseFile("path_to_your_file.txt")

    println(s"Modified Nodes: $modifiedNodes")
    println(s"Removed Nodes: $removedNodes")
    println(s"Added Node Keys: $addedNodeKeys")
  }

  def parseFile(filePath: String): (Set[Int], Set[Int], Set[Int]) = {
    val fileContent = Source.fromFile(filePath).mkString

    val modifiedNodes = extractList("Nodes:", "Modified:", fileContent)
    val removedNodes = extractList("Nodes:", "Removed:", fileContent)
    val addedNodeKeys = extractMapKeys("Nodes:", "Added:", fileContent)

    (modifiedNodes, removedNodes, addedNodeKeys)
  }

  private def extractList(section: String, category: String, content: String): Set[Int] = {
    val pattern = new Regex(s"(?s)$section.*?$category\\s*\\[([^\\]]+)\\]")
    pattern.findFirstMatchIn(content) match {
      case Some(m) => m.group(1).split(",").map(_.trim.toInt).toSet
      case None => Set.empty
    }
  }

  private def extractMapKeys(section: String, category: String, content: String): Set[Int] = {
    val pattern = new Regex(s"(?s)$section.*?$category((?:\\s*[0-9]+:\\s*[0-9]+\\s*)+)")
    pattern.findFirstMatchIn(content) match {
      case Some(m) =>
        m.group(1).trim.split("\n").map { line =>
          line.trim.split(":").head.trim.toInt
        }.toSet
      case None => Set.empty
    }
  }
}
