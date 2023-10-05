import GraphSharder.loadGraphFromHDFS
import org.apache.hadoop.mapreduce.Mapper
import org.apache.hadoop.io.{LongWritable, Text}
import org.slf4j.LoggerFactory

class SimilarityMapper extends Mapper[LongWritable, Text, Text, Text] {

  private type Node = Int
  private type Graph = Map[Node, Set[Node]]
  private val logger = LoggerFactory.getLogger(this.getClass)

  var originalGraph: Graph = _
  var perturbedGraph: Graph = _
  val simRank = new SimRank()

  override def setup(context: Mapper[LongWritable, Text, Text, Text]#Context): Unit = {
    val cachedFiles = context.getCacheFiles.map(_.getPath)
    logger.info(s"Cached files: ${cachedFiles.mkString(", ")}")
    if (cachedFiles.length < 2) {
      logger.error("Not enough cached files available.")
      return
    }
    try {
      originalGraph = loadGraphFromHDFS(cachedFiles(0))
      perturbedGraph = loadGraphFromHDFS(cachedFiles(1))
    } catch {
      case e: Exception =>
        logger.error(s"Error loading graphs: ${e.getMessage}", e)
    }
  }

  override def map(key: LongWritable, value: Text, context: Mapper[LongWritable, Text, Text, Text]#Context): Unit = {
    logger.info(s"Processing value: $value")
    val nodes = value.toString.split(",")
    if (nodes.length == 2) {
      val node1 = nodes(0).trim.toInt
      val node2 = nodes(1).trim.toInt

      // Compute similarity
      val similarity = simRank.jaccardSimilarity(originalGraph, node1, perturbedGraph, node2, depth = 9) // Adjust depth as needed
      if (similarity >= 0.95) {
        // Write the output as <node1, node2> -> similarity
        context.write(new Text(s"$node1, $node2"), new Text(similarity.toString))
      }
    }
  }
}
