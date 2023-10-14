package com.lsc

import scala.io.Source
import scala.util.matching.Regex

/**
 * Object responsible for loading a golden set of modifications from a file.
 */
object GoldenSetLoader {

  /**
   * Parse a file to extract the modified, removed, and added node keys.
   *
   * @param filePath The path to the file containing the golden set data.
   * @return A tuple containing sets of modified, removed, and added node keys.
   */
  def parseFile(filePath: String): (Set[Int], Set[Int], Set[Int]) = {
    val fileContent = Source.fromFile(filePath).mkString

    val modifiedNodes = extractList("Nodes:", "Modified:", fileContent)
    val removedNodes = extractList("Nodes:", "Removed:", fileContent)
    val addedNodeKeys = extractMapKeys("Nodes:", "Added:", fileContent)

    (modifiedNodes, removedNodes, addedNodeKeys)
  }

  /**
   * Extract a list of integers from a specific section and category of the content.
   *
   * @param section The section name within the content.
   * @param category The category name within the section.
   * @param content The string content to extract the list from.
   * @return A set of integers extracted from the specified section and category.
   */
  private def extractList(section: String, category: String, content: String): Set[Int] = {
    val pattern = new Regex(s"(?s)$section.*?$category\\s*\\[([^\\]]+)\\]")
    pattern.findFirstMatchIn(content) match {
      case Some(m) => m.group(1).split(",").map(_.trim.toInt).toSet
      case None => Set.empty
    }
  }

  /**
   * Extract map keys from a specific section and category of the content.
   *
   * @param section The section name within the content.
   * @param category The category name within the section.
   * @param content The string content to extract the map keys from.
   * @return A set of map keys (integers) extracted from the specified section and category.
   */
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
