import com.typesafe.config.ConfigFactory
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path

object ConfigurationLoader {
  private val config = ConfigFactory.load()
  private val debug = config.getBoolean("app.debug")
  private val appConfig = if (debug) config.getConfig("app.debugConfig") else config.getConfig("app")

  // Initialize Hadoop Configuration
  private def getHadoopConfiguration: Configuration = {
    val hadoopConf = new Configuration()
    hadoopConf.addResource(new Path(config.getString("hadoop.coreSitePath")))
    hadoopConf.addResource(new Path(config.getString("hadoop.hdfsSitePath")))
    hadoopConf.set("fs.defaultFS", "hdfs://127.0.0.1:9000")
    hadoopConf
  }

  def getOriginalGraphFilePath: String = config.getString("app.originalGraphFilePath")
  def getPerturbedGraphFilePath: String = config.getString("app.perturbedGraphFilePath")
  def getGoldenSetFilePath: String = config.getString("app.yamlFilePath")
  def getShardFolder: String = config.getString("app.shardsFolder")
  def getNumPieces: Int = appConfig.getInt("numPieces")
  def getFileSystem: FileSystem = FileSystem.get(getHadoopConfiguration)

}
