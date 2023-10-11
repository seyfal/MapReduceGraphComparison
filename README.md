# SimRank Project Documentation

## Overview
SimRank is a sophisticated Scala project aimed to perform similarity rankings on data sets. The project's primary goal is to calculate similarities between different data points in a dataset and rank them accordingly.

## Prerequisites

1. **Java**: Ensure that Java 1.8 is installed
2. **Scala**: The project uses Scala version `3.2.2`.
3. **SBT (Scala Build Tool)**: Ensure SBT is installed.
4. **Hadoop**: The project requires Hadoop, especially if you intend to process data in the HDFS.

## Setup

1. **Cloning the Project**:
   ```bash
   git clone https://github.com/seyfal/MapReduceGraphComparison.git
   cd SimRank
   ```

2. **Configuring Dependencies**:
   The `build.sbt` file contains all the necessary dependencies:
   - Typesafe Config Library: For configuration handling.
   - Logback Classic Logger: For logging.
   - SLF4J API Module: As a logging facade.
   - ScalaTest: For unit testing.
   - Hadoop Client: For Hadoop-related operations.

## Configuration
Configuration for SimRank is essential for successful execution. The following are the key parts:

1. **App Configuration**: Defined in `application.conf` or any custom configuration file. It contains:
   - Debug Mode
   - Common Configuration: Includes pieces, similarity threshold, and depth.
   - Local Configuration: Paths to files when running in a local environment.
   - Cloud Configuration: Paths to files when running on a cloud environment like AWS.
   
2. **Hadoop Configuration**:
   - `coreSitePath`: Path to Hadoop's core-site.xml.
   - `hdfsSitePath`: Path to Hadoop's hdfs-site.xml.

3. **HDFS Configuration**:
   - User directory, paths for graphs, and HDFS base URL.

4. **Job Configuration**:
   - `jarPath`: Path to the assembled JAR for the job.

## Building the Project
To compile and package the project:
```bash
sbt clean compile assembly
```

This command will create a JAR file named `SimRank.jar` in the `target/scala-3.2.2/` directory.

## Deployment and Execution
1. **Local Execution**:
   Adjust the local configuration paths as per your system. Then execute:
   ```bash
   sbt clean compile run
   ```

2. **Cloud/Hadoop Cluster Execution**:
   Ensure Hadoop is properly set up, and adjust the cloud configuration paths to your S3 bucket or equivalent. Copy the JAR to your cluster:
   ```bash
   // modifications to code for the one line execution are in progress 
   ```

   See video for the walkthru of the cloud deployment 

## Results

Upon successful execution, the program will output similarity rankings. These results are influenced by:
- The `similarityThreshold`: Data points with similarities above this threshold will be considered 'similar'.
- The `similarityDepth`: Determines how deep the program looks into the data to determine similarities.

Similarity algorithms needs some modifications as it is highly dependent on the above variables and the nature of graph when it comes to calculating the scores. 

Here is the sample output you can expect after running the application: 

```
[info] ---- Result Explanation ----
[info] In the complex world of graph processing, our program is designed to compare two versions of a graph: the original and its perturbed (or altered) counterpart. Let's dive into the results to understand the effectiveness of our algorithm:
[info] 1. **Traceability Links (TL)**
[info]    A 'traceability link' represents a match between components (nodes or edges) of the original graph and its perturbed version. Our goal is to determine how many of these matches our algorithm correctly identified, and where it may have made mistakes.
[info] 2. **Good and Bad TLs**
[info]    All matches (RTL) we've identified are categorized into two groups: Good Traceability Links (GTL), which are the correct matches, and Bad Traceability Links (BTL), which indicate errors in our algorithm's matching.
[info] 3. **Detailed Metrics:**
[info]    - ACC (0.3333333333333333): Indicates the accuracy of our algorithm. It represents the fraction of matches that our algorithm correctly identified. Higher values are indicative of better accuracy!
[info]    - VPR (0.6666666666666666): Similar to precision, this metric reflects the quality of our matches. A value of 1 would mean perfection, whereas 0 would indicate all matches were erroneous.
[info]    - BTLR (0.0): This metric tells us the fraction of total matches that were mistakes. Ideally, we aim for lower values here.
[info] 4. **To Summarize**
[info]    Our program has been designed to intelligently match components between two versions of a graph. The metrics provided offer insight into its accuracy and precision, ensuring you can trust and act on the results it provides.
[info] Thank you for trusting our solution. We continually strive for accuracy and excellence in graph processing!
[info] ---- End of Explanation ----
```

## Video and Further Documentation

For a detailed walkthrough, watch our [demo video](<Link-To-Video>). In Depth documentation is on the way. 

## Limitations

1. **Memory Consumption**: Due to JVM heap size limits (`-Xmx2G`), datasets that require substantial memory might face issues.
2. **Single Threshold Level**: The application uses a single `similarityThreshold`, which may not be optimal for all use-cases.
3. **Unreliable Algorithm**
4. **Cloud and LocalHost versions have not been combined** 

**Note**: If you need more details or face any issues setting up, feel free to reach out. I'am here to help!
