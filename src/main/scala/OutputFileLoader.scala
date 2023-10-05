import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import scala.io.Source

object OutputFileLoader {

  def loadOutputFile(hdfsPath: String): Map[(Int, Int), Double] = {
    val conf = new Configuration()
    conf.set("fs.defaultFS", "hdfs://localhost:9000/")
    val fs = FileSystem.get(conf)
    val path = new Path(hdfsPath)
    val stream = fs.open(path)
    val source = Source.createBufferedSource(stream)

    val outputData = source.getLines().map { line =>
      val parts = line.split("\t")
      val nodes = parts(0).split(", ").map(_.trim.toInt)
      val score = parts(1).toDouble

      (nodes(0), nodes(1)) -> score
    }.toMap

    source.close()
    outputData
  }
}
