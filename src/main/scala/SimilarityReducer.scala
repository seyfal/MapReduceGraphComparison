package com.lsc

import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Reducer
import org.slf4j.LoggerFactory

import java.util.Iterator

/** A Reducer class to determine the highest similarity for each node.
  */
class SimilarityReducer extends Reducer[Text, Text, Text, Text] {

  // Logger instance
  private val logger = LoggerFactory.getLogger(this.getClass)

  /** Reduce method to compute the maximum similarity value from the given list of values.
    *
    * @param key The node pair as the key.
    * @param values An iterable of similarity values associated with the key.
    * @param context The context to write the results.
    */
  override def reduce(
      key: Text,
      values: java.lang.Iterable[Text],
      context: Reducer[Text, Text, Text, Text]#Context
  ): Unit = {

    logger.info(s"Reducing for key: ${key.toString}")
    val maxSimilarity = getMaxSimilarity(values.iterator())

    logger.debug(s"Maximum similarity for key ${key.toString}: $maxSimilarity")
    context.write(key, new Text(maxSimilarity.toString))
  }

  /** Computes the maximum similarity in a recursive manner.
    *
    * @param iter The iterator for the similarity values.
    * @param currentMax The current maximum value computed so far.
    * @return The maximum similarity value.
    */
  private def getMaxSimilarity(
      iter: Iterator[Text],
      currentMax: Double = 0.0
  ): Double = {
    if (!iter.hasNext) currentMax
    else {
      val nextValue = iter.next().toString.toDouble
      getMaxSimilarity(iter, Math.max(currentMax, nextValue))
    }
  }
}
