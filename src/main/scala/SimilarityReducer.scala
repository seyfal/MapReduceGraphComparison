import GraphSharder.loadGraphFromHDFS
import org.apache.hadoop.mapreduce.Reducer
import org.apache.hadoop.io.{LongWritable, Text}
import org.slf4j.LoggerFactory

class SimilarityReducer extends Reducer[Text, Text, Text, Text] {

  // Reducer to get the highest similarity for each node
  override def reduce(key: Text, values: java.lang.Iterable[Text], context: Reducer[Text, Text, Text, Text]#Context): Unit = {
    var maxSimilarity = 0.0

    values.forEach { value =>
      val similarity = value.toString.toDouble
      if (similarity > maxSimilarity) {
        maxSimilarity = similarity
      }
    }

    context.write(key, new Text(maxSimilarity.toString))
  }
}
