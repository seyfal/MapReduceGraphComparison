package com.lsc

import com.typesafe.config.ConfigFactory
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.slf4j.LoggerFactory

import java.net.URI
import scala.util.Try

/** A singleton object that provides methods to load various configurations from application's configuration file.
  */
object ConfigurationLoader {

  private val logger = LoggerFactory.getLogger(this.getClass)

  // ################### CONFIG LOADER ###################
  // Load configurations using typesafe Config
  private val config = ConfigFactory.load()
  logger.info("Configuration set up successfully")

  // ################### ENVIRONMENT CHECKER ###################

  // Check if the application is being run on the AWS
  logger.info("Checking if the application is running on the AWS")
  private val isOnAWS = Try {
    val uri = new URI("http://169.254.169.254/latest/meta-data/")
    val url = uri.toURL()
    val connection = url.openConnection()
    connection.setConnectTimeout(3000) // set timeout to 5 seconds
    connection.setReadTimeout(3000) // set read timeout to 5 seconds

    val source = scala.io.Source.fromInputStream(connection.getInputStream)
    try {
      source.mkString.nonEmpty
    } finally {
      source.close()
    }
  }.getOrElse(false)
  logger.info(s"Is the application running on the AWS? $isOnAWS")

  // Check if the application is being run on the AWS EMR
  logger.info("Checking if the application is running on the AWS EMR")
  private val isOnEMR = Try {
    val uri = new URI("http://169.254.169.254/latest/user-data/")
    val url = uri.toURL()
    val connection = url.openConnection()
    connection.setConnectTimeout(3000) // set timeout to 5 seconds
    connection.setReadTimeout(3000) // set read timeout to 5 seconds

    val source = scala.io.Source.fromInputStream(connection.getInputStream)
    try {
      val userData = source.mkString
      userData.contains("emr-apps")
    } finally {
      source.close()
    }
  }.getOrElse(false)
  logger.info(s"Is the application running on the AWS EMR? $isOnEMR")

  // set the environment
  private val environment = if (isOnAWS && isOnEMR) "cloud" else "local"
  logger.info(s"Environment set to $environment")

  // ################### DEBUG CHECKER ###################
  // Determine if the application is running in debug mode
  private val debug = config.getBoolean("app.debug")
  logger.info(s"Is the application running in debug mode? $debug")

  // ################### FINAL PATHS SETTING ###################
  // Load appropriate configuration section based on debug mode
  logger.info(
    "Loading appropriate configuration section based on debug and environment settings"
  )
  private val appConfig =
    if (debug && environment == "local") {
      config.getConfig("app.local.debug")
    } else if (!debug && environment == "local") {
      config.getConfig("app.local.release")
    } else if (environment == "cloud") {
      config.getConfig("app.cloud")
    } else {
      throw new IllegalArgumentException(
        "Invalid configuration or environment settings"
      )
    }
  logger.info(
    "Configuration is set to take the path from the following section: " + appConfig
      .root()
      .render()
  )

  // ################### HDFS ###################
  def getHdfsBase: String = config.getString("hdfs.hdfsBase")
  def getOriginalGraphPath: String = config.getString("hdfs.originalGraphPath")
  def getPerturbedGraphPath: String = config.getString("hdfs.perturbedGraphPath")
  def getUserDirectory: String = config.getString("hdfs.userDirectory")
  def getJobPath: String = config.getString("job.jarPath")

  // ################### FILE PATHS ###################
  // Retrieve the original graph file path
  def getOriginalGraphFilePath: String = {
    appConfig.getString("originalGraphFilePath")
  }

  // Retrieve the perturbed graph file path
  def getPerturbedGraphFilePath: String =
    appConfig.getString("perturbedGraphFilePath")

  // Retrieve the golden set file path (YAML)
  def getGoldenSetFilePath: String = appConfig.getString("yamlFilePath")

  // ################### GET SIMILARITY CONFIGURATIONS ###################
  // Retrieve the number of pieces (shards) for the graph
  def getNumPieces: Int = config.getInt("app.common.numPieces")

  // Retrieve the similarity threshold value for comparisons
  def getSimilarityThreshold: Double =
    config.getDouble("app.common.similarityThreshold")

  // Retrieve the depth for similarity checks
  def getSimilarityDepth: Int = config.getInt("app.common.similarityDepth")

  // Get the Hadoop FileSystem instance.
  def getFileSystem: FileSystem = FileSystem.get(getHadoopConfiguration)

  // ################### HADOOP CONFIG ###################
  // Initialize and return the Hadoop configuration.
  private def getHadoopConfiguration: Configuration = {
    val hadoopConf = new Configuration()
    // Adding core-site.xml path to Hadoop's configuration
    hadoopConf.addResource(new Path(config.getString("hadoop.coreSitePath")))
    // Adding hdfs-site.xml path to Hadoop's configuration
    hadoopConf.addResource(new Path(config.getString("hadoop.hdfsSitePath")))
    // Set default file system
    hadoopConf.set("fs.defaultFS", "hdfs://127.0.0.1:9000")
    hadoopConf
  }

}
