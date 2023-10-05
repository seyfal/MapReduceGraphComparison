import org.slf4j.LoggerFactory

import java.io.{BufferedWriter, FileWriter, IOException, OutputStreamWriter}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FSDataInputStream, FSDataOutputStream, FileSystem, Path}

import java.text.SimpleDateFormat
import java.util.Date

object GraphSharder {

  private val logger = LoggerFactory.getLogger(this.getClass)
  type Node = Int
  type Graph = Map[Node, Set[Node]]
  type GraphPieces = Array[Graph]
  type Shard = Map[Graph, Graph]

  private def generateFilesForPieces(pieces: GraphPieces, outputDir: String): Unit = {
    val methodName = "generateFilesForPieces"
    logger.info(s"$methodName: Generating files for pieces...")
    pieces.zipWithIndex.foreach { case (piece, index) =>
      val filename = s"$outputDir/shard${index + 1}.txt" // change extension to txt
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
    logger.info(s"$methodName: Files for pieces have been generated successfully in $outputDir.")
  }

  // TODO: Implement louvain algorithm to divide the graph into pieces in the future.
  def divideGraph(graph: Graph, numPieces: Int, outputDir: String, generateFiles: Boolean): GraphPieces = {
    val methodName = "divideGraph"
    logger.info(s"$methodName: Initiating the division process into $numPieces pieces...")

    val pieces = Array.fill(numPieces)(Map[Node, Set[Node]]())
    logger info(s"$methodName: Initialized ${pieces.size} empty pieces.")

    graph.foreach { case (node, neighbors) =>
      val pieceIndex = math.abs(node.hashCode) % numPieces
      pieces(pieceIndex) += (node -> neighbors)
    }

    if (generateFiles) generateFilesForPieces(pieces, outputDir)
    logger.info(s"$methodName: Pieces have been generated successfully.")

    pieces
  }

  private def combineGraphs(graph1: Graph, graph2: Graph): Graph = graph1 ++ graph2.map {
    case (k, v) => k -> (v ++ graph1.getOrElse(k, Set()))
  }

  def shuffleGraph(originalGraphPieces: Array[Graph], perturbedGraphPieces: Array[Graph]): List[Map[Graph, Graph]] = {
    val methodName = "shuffleGraph"
    logger.info(s"$methodName: Initiating shuffle process...")

    val shuffledGraphList = (for {
      originalPiece <- originalGraphPieces.toList
      perturbedPiece <- perturbedGraphPieces.toList
    } yield Map(originalPiece -> combineGraphs(originalPiece, perturbedPiece)))

    logger.info(s"$methodName: Graph pieces shuffled successfully. Generated ${shuffledGraphList.size} shuffled pieces.")
    shuffledGraphList
  }

  def writeShardsToHDFS(shards: List[Map[Graph, Graph]]): Unit = {
    val methodName = "writeShardsToHDFS"
    logger.info(s"$methodName: Initiating write process to HDFS...")

    val configuration = new Configuration()
    val fs = ConfigurationLoader.getFileSystem

    // Format current time as a string and append it to the base directory name
    val timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
    val hdfsBaseDir = s"/user/seyfal/CS441/shards_directory_$timestamp"

    try {
      shards.zipWithIndex.foreach { case (shard, index) =>
        val hdfsDir = new Path(s"$hdfsBaseDir/shard${index + 1}")
        fs.mkdirs(hdfsDir) // create directory for each shard

        val hdfsPath = new Path(s"$hdfsDir/shard${index + 1}.txt")
        val outputStream: FSDataOutputStream = fs.create(hdfsPath)
        try {
          shard.foreach { case (graph1, graph2) =>
            val graph1Str = graph1.map { case (node, neighbors) => s"$node -> ${neighbors.mkString(", ")}" }.mkString("\n")
            val graph2Str = graph2.map { case (node, neighbors) => s"$node -> ${neighbors.mkString(", ")}" }.mkString("\n")
            outputStream.writeUTF(s"$graph1Str\n$graph2Str\n")
          }
        } finally {
          outputStream.close()
        }

        logger.info(s"$methodName: Wrote shard${index + 1} to $hdfsDir")
      }
    } catch {
      case e: IOException =>
        logger.error(s"$methodName: Error writing shards to HDFS", e)
    } finally {
      fs.close()
    }

    logger.info(s"$methodName: Shards have been written successfully to $hdfsBaseDir.")
  }

  def writeShardsToHDFS_nodes(shards: List[Map[Graph, Graph]]): String = {
    val methodName = "writeShardsToHDFS"
    logger.info(s"$methodName: Initiating write process to HDFS...")

    val configuration = new Configuration()
    val fs = ConfigurationLoader.getFileSystem

    val timestamp = new SimpleDateFormat("HH.mm.ss_dd.MM.yyyy").format(new Date())
    val hdfsBaseDir = s"/user/seyfal/CS441/shards_directory_$timestamp"
    // TODO: Change the hardcoded base directory
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
              logger.debug(s"Wrote mapping $origNode -> $pertNode to shard${index + 1}")
            }
          }
        } catch {
          case e: Exception =>
            logger.error(s"$methodName: Error while writing to shard ${index + 1}", e)
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

    logger.info(s"$methodName: Shards have been written successfully to $hdfsBaseDir.")

    hdfsBaseDir
  }

  private def serializeGraph(graph: Graph): String = {
    graph.map { case (node, neighbors) =>
      s"$node -> ${neighbors.mkString(", ")}"
    }.mkString("\n")
  }

  // TODO: Implement Avro or Protobuf serialization in the future.
  def storeGraphInHDFS(graph: Graph, hdfsPathStr: String): Unit = {
    val methodName = "storeGraphInHDFS"
    logger.info(s"$methodName: Storing the graph to HDFS...")

    val configuration = new Configuration()
    val fs = ConfigurationLoader.getFileSystem
    val hdfsPath = new Path(hdfsPathStr)

    val serializedGraph = serializeGraph(graph)

    val outputStream: FSDataOutputStream = fs.create(hdfsPath, true) // Overwriting if exists
    try {
      outputStream.writeUTF(serializedGraph)
    } catch {
      case e: IOException =>
        logger.error(s"$methodName: Error writing graph to HDFS", e)
    } finally {
      outputStream.close()
    }

    logger.info(s"$methodName: Graph has been written successfully to $hdfsPath.")
  }

  private def deserializeGraph(serializedData: String): Graph = {
    val lines = serializedData.split("\n")

    lines.map { line =>
      val parts = line.split(" -> ")
      val node = parts(0).toInt
      val neighbors = if (parts.length > 1) parts(1).split(", ").map(_.toInt).toSet else Set[Int]()
      node -> neighbors
    }.toMap
  }

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
}
