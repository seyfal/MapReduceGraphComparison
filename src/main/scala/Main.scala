import Explanations.explainResults
import GoldenSetLoader.{parseFile, extractList, extractMapKeys}
import GraphSharder.{divideGraph, shuffleGraph, storeGraphInHDFS, writeShardsToHDFS_nodes}
import OutputFileLoader.loadOutputFile
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapreduce.Job
import org.slf4j.LoggerFactory
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat

import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date

object Main {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val methodName = "Main"
  private type Node = Int
  private type Graph = Map[Node, Set[Node]]
  private type GraphPieces = List[Graph]

  def main(args: Array[String]): Unit = {
    try {
      
      logger.info(s"$methodName: Starting the program")
      
      logger.info(s"$methodName: Loading the configuration file")
      
      logger.info(s"$methodName: Loading the original graph")
      val originalGraphFilePath = ConfigurationLoader.getOriginalGraphFilePath
      logger.info(s"$methodName: Loading the perturbed graph")
      val perturbedGraphFilePath = ConfigurationLoader.getPerturbedGraphFilePath
      logger.info(s"$methodName: Loading the shard folder")
      val shardFolder = ConfigurationLoader.getShardFolder
      logger.info(s"$methodName: Loading the number of pieces")
      val numPieces = ConfigurationLoader.getNumPieces
      logger.info(s"$methodName: Loading the golden set path")
      val goldenSetPath = ConfigurationLoader.getGoldenSetFilePath

      logger.info(s"$methodName: Dividing the original graph into pieces")
      val originalGraph = GraphLoader.loadGraph(originalGraphFilePath)
      logger.info(s"$methodName: Dividing the perturbed graph into pieces")
      val perturbedGraph = GraphLoader.loadGraph(perturbedGraphFilePath)
      
      // TODO: Update the output directory

      logger.info(s"$methodName: Dividing the original graph into $numPieces pieces")
      val originalPieces = divideGraph(originalGraph, numPieces, outputDir = shardFolder, false)
      logger.info(s"$methodName: Dividing the perturbed graph into $numPieces pieces")
      val perturbedPieces = divideGraph(perturbedGraph, numPieces, outputDir = shardFolder, false)

      logger.info(s"$methodName: Shuffling the original graph")
      val shuffledGraph = shuffleGraph(originalPieces, perturbedPieces)

      logger.info(s"$methodName: Upload the shards to the HDFS and save the directory path")
      val hdfsDirectoryPath = writeShardsToHDFS_nodes(shuffledGraph)
      logger.info(s"$methodName: The shards are uploaded to the HDFS and the directory path is $hdfsDirectoryPath")

      logger.info(s"$methodName: Upload the original graph to the HDFS")
      storeGraphInHDFS(originalGraph, "/user/seyfal/CS441/originalGraph.txt")

      logger.info(s"$methodName: Upload the perturbed graph to the HDFS")
      storeGraphInHDFS(perturbedGraph, "/user/seyfal/CS441/perturbedGraph.txt")

      logger.info(s"$methodName: Configuring the MapReduce job")
      val job = Job.getInstance(new Configuration(), "NodeSimilarityJob")
      job.setJar("/Users/seyfal/Desktop/CS441 Cloud/SimRank/SimRank/target/scala-3.3.1/simrank_3-0.1.0-SNAPSHOT.jar")

      logger.info(s"$methodName: Adding the original and perturbed graphs to the cache")
      job.addCacheFile(new URI("hdfs://localhost:9000/user/seyfal/CS441/originalGraph.txt"))
      job.addCacheFile(new URI("hdfs://localhost:9000/user/seyfal/CS441/perturbedGraph.txt"))

      // Specify Mapper (and optionally Reducer) class
      job.setMapperClass(classOf[SimilarityMapper])
      job.setReducerClass(classOf[SimilarityReducer])

      // Set output key and value types (from Mapper and Reducer)
      job.setOutputKeyClass(classOf[Text])
      job.setOutputValueClass(classOf[Text])

      // Input and Output formats
      logger.info(s"$methodName: Setting the input and output formats")
      FileInputFormat.addInputPath(job, new Path(s"hdfs://localhost:9000$hdfsDirectoryPath"))
      logger.info(s"$methodName: Setting the output format")

      val outputTimestamp = new SimpleDateFormat("HH.mm.ss_dd.MM.yyyy").format(new Date())
      val outputPath = s"hdfs://localhost:9000/user/seyfal/CS441//output_$outputTimestamp"
      FileOutputFormat.setOutputPath(job, new Path(outputPath))

      // Run the job
      val success = job.waitForCompletion(true)
      if (success) {
        logger.info(s"$methodName: MapReduce job completed successfully")
      } else {
        logger.error(s"$methodName: MapReduce job failed")
      }

      // load your reducer's output
      val outputData = loadOutputFile(s"$outputPath/part-r-00000")
      logger.info(s"$methodName: Output file is loaded and the size is ${outputData.size}")

      val (modifiedNodes, removedNodes, addedNodeKeys) = parseFile(goldenSetPath)

      val nodesFromOutput = outputData.keys.map(_._1).toSet
      val mySameNodes = nodesFromOutput.filter(node => outputData((node, node)) == 1.0)
      val myModifiedNodes = nodesFromOutput.filter(node => outputData((node, node)) < 1.0)

      println(s"Modified nodes: $modifiedNodes")
      println(s"Removed nodes: $removedNodes")
      println(s"Added nodes: $addedNodeKeys")
      println(s"My same nodes: $mySameNodes")
      println(s"My modified nodes: $myModifiedNodes")

      // Compute GTL, BTL, ATL, DTL, CTL, WTL

      // Compute CTL
      val CTL = modifiedNodes.count(node => !mySameNodes.contains(node) && !addedNodeKeys.contains(node))

      // Compute WTL
      val WTL = myModifiedNodes.count(node => !modifiedNodes.contains(node) && !addedNodeKeys.contains(node))

      // Compute DTL
      val DTL = removedNodes.count(node => !nodesFromOutput.contains(node))

      // Compute ATL
      val ATL = mySameNodes.count(node => modifiedNodes.contains(node))

      val GTL = DTL + ATL
      val BTL = CTL + WTL
      val RTL = GTL + BTL

      // print the intermediate statistics
      println(s"ATL: $ATL")
      println(s"DTL: $DTL")
      println(s"WTL: $WTL")
      println(s"CTL: $CTL")
      println(s"RTL: $RTL")
      println(s"GTL: $GTL")
      println(s"BTL: $BTL")

      // Compute ACC, VPR, and BTLR
      val ACC = ATL.toDouble / RTL
      val BTLR = WTL.toDouble / RTL
      val VPR = (GTL - BTL).toDouble / (2 * RTL) + 0.5

      explainResults(ACC, VPR, BTLR)

    } catch {
      case e: Exception =>
        logger.error(s"Error occurred in Main: ", e)
    }
  }
  
}
