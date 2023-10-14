package com.lsc

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FSDataInputStream, FSDataOutputStream, Path}
import org.slf4j.LoggerFactory

import java.io.{BufferedWriter, FileWriter, IOException}
import java.text.SimpleDateFormat
import java.util.Date

/** Object responsible for sharding graph data, managing HDFS I/O operations related to graph storage.
  */
object GraphSharder {

  // Type Aliases for better code readability.
  type Node = Int
  type Graph = Map[Node, Set[Node]]
  type Shard = Map[Graph, Graph]
  private type GraphPieces = Array[Graph]
  private val logger = LoggerFactory.getLogger(this.getClass)

  /** Method to divide graph into multiple pieces.
    * Note: Plan to use the Louvain algorithm to divide the graph in future versions.
    *
    * @param graph Input graph.
    * @param numPieces Number of pieces the graph should be divided into.
    * @return Array of graph pieces.
    */
  def divideGraph(graph: Graph, numPieces: Int): GraphPieces = {
    val methodName = "divideGraph"
    logger.info(
      s"$methodName: Initiating the division process into $numPieces pieces..."
    )

    val pieces = Array.fill(numPieces)(Map[Node, Set[Node]]())
    logger.info(s"$methodName: Initialized ${pieces.length} empty pieces.")

    graph.foreach { case (node, neighbors) =>
      val pieceIndex = math.abs(node.hashCode) % numPieces
      pieces(pieceIndex) += (node -> neighbors)
    }

    logger.info(s"$methodName: Pieces have been generated successfully.")

    pieces
  }

  /** Shuffle two sets of graph pieces.
    *
    * @param originalGraphPieces Original graph pieces.
    * @param perturbedGraphPieces Perturbed graph pieces.
    * @return List of mapped graphs.
    */
  def shuffleGraph(
      originalGraphPieces: Array[Graph],
      perturbedGraphPieces: Array[Graph]
  ): List[Map[Graph, Graph]] = {
    val methodName = "shuffleGraph"
    logger.info(s"$methodName: Initiating shuffle process...")

    val shuffledGraphList = for {
      originalPiece <- originalGraphPieces.toList
      perturbedPiece <- perturbedGraphPieces.toList
    } yield Map(originalPiece -> combineGraphs(originalPiece, perturbedPiece))

    logger.info(
      s"$methodName: Graph pieces shuffled successfully. Generated ${shuffledGraphList.size} shuffled pieces."
    )
    shuffledGraphList
  }

  /** Private method to combine two graphs.
    *
    * @param graph1 First graph.
    * @param graph2 Second graph.
    * @return Merged graph.
    */
  private def combineGraphs(graph1: Graph, graph2: Graph): Graph =
    graph1 ++ graph2.map { case (k, v) =>
      k -> (v ++ graph1.getOrElse(k, Set()))
    }

  /** Writes the graph shards to HDFS, specifically the nodes mapping between original and perturbed graphs.
    *
    * @param shards List of graph shards with mappings between original and perturbed graphs.
    * @return The base directory in HDFS where the shards were written.
    */
  def writeShardsToHDFS_nodes(shards: List[Map[Graph, Graph]]): String = {
    val methodName = "writeShardsToHDFS_nodes"
    logger.info(s"$methodName: Initiating write process to HDFS...")

    val configuration = new Configuration()
    val fs = ConfigurationLoader.getFileSystem

    val timestamp =
      new SimpleDateFormat("HH.mm.ss_dd.MM.yyyy").format(new Date())
    val hdfsBaseDir = s"/user/seyfal/CS441/shards_directory_$timestamp"
    // TODO: Possibly change the hardcoded base directory
    // val hdfsBaseDir = s"$baseDir/shards_directory_$timestamp"

    try {
      shards.zipWithIndex.foreach { case (shard, index) =>
        val hdfsPath = new Path(s"$hdfsBaseDir/shard${index + 1}.csv")
        val outputStream: FSDataOutputStream = fs.create(hdfsPath)

        try {
          shard.foreach { case (originalGraph, perturbedGraph) =>
            // Assuming both the original and perturbed graph have the same number of keys
            val pairedNodes = originalGraph.keys.zip(perturbedGraph.keys)

            pairedNodes.foreach { case (origNode, pertNode) =>
              outputStream.writeUTF(s"$origNode,$pertNode\n")
              logger.debug(
                s"Wrote mapping $origNode -> $pertNode to shard${index + 1}"
              )
            }
          }
        } catch {
          case e: Exception =>
            logger.error(
              s"$methodName: Error while writing to shard ${index + 1}",
              e
            )
        } finally {
          outputStream.close()
        }

        logger.info(s"$methodName: Wrote shard${index + 1} to $hdfsPath")
      }
    } catch {
      case e: IOException =>
        logger.error(s"$methodName: Error writing shards to HDFS", e)
    } finally {
      fs.close()
    }

    logger.info(
      s"$methodName: Shards have been written successfully to $hdfsBaseDir."
    )

    hdfsBaseDir
  }

  /** Stores a given graph in HDFS.
    *
    * @param graph The graph to store.
    * @param hdfsPathStr The HDFS path to store the graph.
    */
  def storeGraphInHDFS(graph: Graph, hdfsPathStr: String): Unit = {
    val methodName = "storeGraphInHDFS"
    logger.info(s"$methodName: Storing the graph to HDFS...")

    val configuration = new Configuration()
    val fs = ConfigurationLoader.getFileSystem
    val hdfsPath = new Path(hdfsPathStr)

    val serializedGraph = serializeGraph(graph)

    val outputStream: FSDataOutputStream =
      fs.create(hdfsPath, true) // Overwriting if exists
    try {
      outputStream.writeUTF(serializedGraph)
    } catch {
      case e: IOException =>
        logger.error(s"$methodName: Error writing graph to HDFS", e)
    } finally {
      outputStream.close()
    }

    logger.info(
      s"$methodName: Graph has been written successfully to $hdfsPath."
    )
  }

  /** Serializes a given graph into a string format.
    *
    * @param graph The graph to serialize.
    * @return A serialized string representation of the graph.
    */
  private def serializeGraph(graph: Graph): String = {
    graph
      .map { case (node, neighbors) =>
        s"$node -> ${neighbors.mkString(", ")}"
      }
      .mkString("\n")
  }

  // TODO: Implement Avro or Protobuf serialization in the future.

  /** Loads a graph from HDFS.
    *
    * @param hdfsPathStr The HDFS path to load the graph from.
    * @return The loaded graph.
    */
  def loadGraphFromHDFS(hdfsPathStr: String): Graph = {
    val methodName = "loadGraphFromHDFS"
    logger.info(s"$methodName: Loading the graph from HDFS...")

    val configuration = new Configuration()
    val fs = ConfigurationLoader.getFileSystem
    val hdfsPath = new Path(hdfsPathStr)

    val inputStream: FSDataInputStream = fs.open(hdfsPath)
    try {
      val serializedData = inputStream.readUTF()
      deserializeGraph(serializedData)
    } catch {
      case e: IOException =>
        logger.error(s"$methodName: Error reading graph from HDFS", e)
        Map.empty[Node, Set[Node]]
    } finally {
      inputStream.close()
    }
  }

  /** Deserializes a string representation of a graph into a Graph.
    *
    * @param serializedData The serialized string representation of the graph.
    * @return The deserialized graph.
    */
  private def deserializeGraph(serializedData: String): Graph = {
    val lines = serializedData.split("\n")

    lines.map { line =>
      val parts = line.split(" -> ")
      val node = parts(0).toInt
      val neighbors =
        if (parts.length > 1) parts(1).split(", ").map(_.toInt).toSet
        else Set[Int]()
      node -> neighbors
    }.toMap
  }

  /** Private method to generate files for graph pieces.
    *
    * @param pieces Array of graph pieces.
    * @param outputDir Directory path where files will be stored.
    */
  private def generateFilesForPieces(
      pieces: GraphPieces,
      outputDir: String
  ): Unit = {
    val methodName = "generateFilesForPieces"
    logger.info(s"$methodName: Generating files for pieces...")

    pieces.zipWithIndex.foreach { case (piece, index) =>
      val filename =
        s"$outputDir/shard${index + 1}.txt" // change extension to txt
      val writer = new BufferedWriter(new FileWriter(filename))
      try {
        piece.foreach { case (node, neighbors) =>
          writer.write(s"$node -> ${neighbors.mkString(", ")}\n")
        }
      } catch {
        case e: IOException =>
          logger.error(s"$methodName: Error writing to file: $filename", e)
      } finally {
        writer.close()
      }
    }
    logger.info(
      s"$methodName: Files for pieces have been generated successfully in $outputDir."
    )
  }
}
