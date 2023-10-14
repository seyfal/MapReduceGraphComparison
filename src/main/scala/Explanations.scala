package com.lsc

object Explanations {
  def explainResults(ACC: Double, VPR: Double, BTLR: Double): Unit = {
    println("---- Result Explanation ----\n")
    println("In the complex world of graph processing, our program is designed to compare two versions of a graph: the original and its perturbed (or altered) counterpart. Let's dive into the results to understand the effectiveness of our algorithm:")

    println("\n1. **Traceability Links (TL)**")
    println("   A 'traceability link' represents a match between components (nodes or edges) of the original graph and its perturbed version. Our goal is to determine how many of these matches our algorithm correctly identified, and where it may have made mistakes.")

    println("\n2. **Good and Bad TLs**")
    println("   All matches (RTL) we've identified are categorized into two groups: Good Traceability Links (GTL), which are the correct matches, and Bad Traceability Links (BTL), which indicate errors in our algorithm's matching.")

    println("\n3. **Detailed Metrics:**")
    println(s"   - ACC ($ACC): Indicates the accuracy of our algorithm. It represents the fraction of matches that our algorithm correctly identified. Higher values are indicative of better accuracy!")
    println(s"   - VPR ($VPR): Similar to precision, this metric reflects the quality of our matches. A value of 1 would mean perfection, whereas 0 would indicate all matches were erroneous.")
    println(s"   - BTLR ($BTLR): This metric tells us the fraction of total matches that were mistakes. Ideally, we aim for lower values here.")

    println("\n4. **To Summarize**")
    println("   Our program has been designed to intelligently match components between two versions of a graph. The metrics provided offer insight into its accuracy and precision, ensuring you can trust and act on the results it provides.")

    println("\n---- End of Explanation ----")
  }
}
