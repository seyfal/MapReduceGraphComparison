![Static Badge](https://img.shields.io/badge/build-passing-brightgreen)

## My Submission: 
1. **Seyfal Sultanov**
2. **NetID: ssulta24**
3. **UIC Email: ssulta24@uic.edu**
4. **UIN: 678686497**

# Distributed Graph Matching: Code Documentation

## Table of Contents
1. [Introduction](#introduction)
2. [System Architecture](#system-architecture)
3. [Code Logic and Flow](#code-logic-and-flow)
   - [Initialization](#initialization)
   - [Data Loading and Pre-processing](#data-loading-and-pre-processing)
   - [Distributed Matching](#distributed-matching)
   - [Post-processing](#post-processing)
   - [Result Compilation](#result-compilation)
4. [Generated Statistics](#generated-statistics)
   - [Overview](#overview)
   - [Matching Metrics](#matching-metrics)
   - [Performance Metrics](#performance-metrics)
   - [Error Metrics](#error-metrics)
   - [Visualization Tools](#visualization-tools)
5. [Areas of Improvement](#areas-of-improvement)
   - [Code Optimization](#code-optimization)
   - [Statistical Methods](#statistical-methods)
   - [Cloud Integration](#cloud-integration)
   - [User Experience](#user-experience)
6. [Known Bugs and Issues](#known-bugs-and-issues)
7. [FAQs](#faqs)
8. [Appendices](#appendices)
   - [Code Listings](#code-listings)
   - [References and Citations](#references-and-citations)
9. [Contact and Support](#contact-and-support)

## Introduction
The primary objective of this project is to leverage cloud-based frameworks and distributed computing technologies to analyze large-scale graph matching challenges. Using the [network simulator, NetGraphSim](https://github.com/0x1DOCD00D/NetGameSim), we generate expansive graphs representative of big data. These graphs undergo specialized perturbation operations to produce their modified counterparts. The key challenge lies in discerning the differences between the original and perturbed graphs. 

This project not only aims to determine these differences but also seeks to quantify the matching accuracy through statistically robust methods. Our results not only offer a holistic matching score for the graphs but also provide insights into how closely individual nodes and edges correlate. While the graph isomorphism problem is NP-complete, our approximation algorithms attempt to solve real-world challenges by comparing nodes and edges, producing meaningful metrics to gauge similarity.

## System Architecture

### Frameworks and Libraries:

The project is rooted in the Scala programming language, leveraging its functional capabilities to handle the complex logic and vast data sets intrinsic to graph analysis. Key libraries and frameworks integral to this project include:

1. **NetGameSim:** An open-source [network simulator](https://github.com/0x1DOCD00D/NetGameSim) used to create and perturb expansive graphs, simulating real-world big data challenges.
   
2. **Logback & SLF4J:** Adopted for sophisticated logging, these tools help monitor, debug, and trace the application, ensuring transparent and efficient execution. More about [Logback](https://logback.qos.ch/) and [SLFL4J](https://www.slf4j.org/).

3. **Typesafe Configuration Library:** Manages configurations, ensuring that all input parameters are seamlessly and accurately integrated into the project. Dive into the [Typesafe Configuration Library](https://github.com/lightbend/config).

4. **Apache Hadoop:** At the heart of the distributed processing, [Apache Hadoop](http://hadoop.apache.org/) empowers the application to analyze massive data sets efficiently. The framework uses the map/reduce model, which splits tasks, processes them in parallel, and then aggregates the results.

## Code Logic and Flow

### Initialization

#### Configuration Loading
The initialization process starts with loading the application configurations from the provided `conf` file. This is managed by the `ConfigurationLoader` singleton object which is a part of the `com.lsc` package. Here are the key aspects of the configuration loading process:

- The [Typesafe Config library](https://github.com/lightbend/config) is used to load the configurations. The configuration structure enables a distinction between running the application in local vs. cloud, and in debug vs. release modes.
  
- The application first checks if it's being run on AWS and if so, on AWS EMR. This is done by attempting to connect to specific AWS metadata endpoints. If these checks are successful, the application sets its environment to "cloud"; otherwise, it defaults to "local".

- After determining the environment, the appropriate file paths and settings are loaded. For instance, when running locally in debug mode, the paths under `app.local.debug` are used, but if the environment is detected as the cloud, the paths under `app.cloud` are utilized.

- The Hadoop and HDFS configurations are loaded based on the specified paths in the `conf` file. 

#### Hadoop Initialization
Once the configuration is loaded, the Hadoop FileSystem instance is fetched using the `getFileSystem` method from the ConfigurationLoader. The Hadoop configuration (`hadoopConf`) adds the specified paths from the `core-site.xml` and `hdfs-site.xml` files and sets the default filesystem.

#### Data Format (for Config file):

```
app {

  debug = false // set to True to execute in debug mode with more data and logger statements

  common {
    numPieces = 2 // number of pieces you want to divide the graph into, will directly affect number of shards, numShards = numPieces^2 
    similarityThreshold = 0.95 // threshold of similarity that mapper will accept to pass data to the reducer
    similarityDepth = 3 // how many neighbours deep you want to analyze the similarity 
  }

  local {
    release {
      originalGraphFilePath = "path to originalGraph release version on your local machine"
      perturbedGraphFilePath = "path to perturbedGraph release version on your local machine"
      yamlFilePath = "path to goldenSet release version on your local machine"
    }
    debug {
      originalGraphFilePath = "path to originalGraph debug version on your local machine"
      perturbedGraphFilePath = "path to perturbedGraph debug version on your local machine"
      yamlFilePath = "path to goldenSet debug version on your local machine"
    }
  }

  cloud {
    originalGraphFilePath = "s3://path to originalGraph"
    perturbedGraphFilePath = "s3://path to perturbedGraph"
    yamlFilePath = "s3://path to the goldenSet"
  }
}

hadoop {
  coreSitePath = "/Users/{your user name}/hadoop-{your hadoop version}/etc/hadoop/core-site.xml" 
  hdfsSitePath = "/Users/{your user name}/hadoop-{your hadoop version}/etc/hadoop/hdfs-site.xml"
  // or if you are running in the cloud:
  coreSitePath = "/etc/hadoop/conf/core-site.xml"
  hdfsSitePath = "/etc/hadoop/conf/hdfs-site.xml" 
}

hdfs {
  userDirectory = "base directory where your files reside"
  originalGraphPath = "path to originalGraph.txt"
  perturbedGraphPath = "path to perturbedGraph.txt" 
  hdfsBase = "hdfs://localhost:9000" // if running on local host, otherwise not needed in the cloud. 
}

job {
    jarPath = "path to jar file, normally in your target/scala{version}/ folder"
}
```

### Data Loading and Pre-processing

The application is designed to handle and process graph data. The graph is expected to be in the Graphviz `.dot` format, which presents data as an adjacency list. Once the graph is loaded into the application, it undergoes various processes like dividing, shuffling, and storing into the Hadoop Distributed File System (HDFS).

#### Step-by-Step Breakdown:

2. **Loading the Graph:**
   - The graph in `.dot` format is read from the specified file path. The `loadGraph` method is responsible for this.
   - The graph is loaded line by line. Each line represents a node and its edges.
   - If any errors occur during this process (like the file not being found, or IO errors), the application logs the error and returns an empty graph.
   
3. **Dividing the Graph:**
   - The loaded graph is then divided into multiple pieces using the `divideGraph` method.
   - The number of pieces to divide into is determined by the configurations loaded earlier.
   - Each node is assigned to a piece based on its hash code. This ensures an even distribution of nodes across pieces.
   
4. **Shuffling the Graph:**
   - Once divided, the pieces of the original graph are shuffled with the pieces of the perturbed graph using the `shuffleGraph` method.
   - The method combines each piece of the original graph with every piece of the perturbed graph. 
   - The `combineGraphs` method merges two graphs, ensuring no node overlaps and retaining all edges.
   
5. **Storing in HDFS:**
   - After shuffling, the resultant shards of the graph are written to HDFS using the `writeShardsToHDFS_nodes` method.
   - Each shard is written as a CSV, with mappings between nodes of the original and perturbed graphs.
   - Alongside the shards, the original and perturbed graphs themselves are also stored in HDFS as cache files using the `storeGraphInHDFS` method.
   - The graph is serialized into a string format using the `serializeGraph` method before storing in HDFS.
   
#### Data Format:

The graph is imported as an adjacency list in the `.dot` format. Here's a simplified example of the input:

```plaintext
digraph "Net Graph with 101 nodes" {
"5" -> "54" ["weight"="6.0"]
"54" -> "37" ["weight"="1.0"]
...
}
```

When the graph is loaded and processed, it is translated into a map of nodes and their corresponding edges, making it more manageable and easy to operate on.

#### Error Handling:

Throughout the data loading and preprocessing stages, there are various checkpoints to ensure error handling:

- If there's an error in reading the file or parsing its content, appropriate error messages are logged.
- IOExceptions, particularly when dealing with HDFS operations, are caught and logged.
- Any unexpected error is also caught and logged, ensuring that the application doesn't crash abruptly.

### Distributed Matching

Distributed Matching is the core of the application, involving the comparison and mapping of nodes between two graphs in a distributed fashion. It utilizes the MapReduce framework, enabling efficient distributed processing of large-scale datasets.

#### Distributed System Configuration

The system first configures the job by assigning it a name and setting up the environment:

```scala
val job = Job.getInstance(new Configuration(), "NodeSimilarityJob")
```

Additionally, it also specifies the JAR file location, which contains the classes required to run the job:

```scala
job.setJar(jobJarPath)
```

To make the original and perturbed graphs available to all nodes in the cluster, they're added to the job's distributed cache:

```scala
job.addCacheFile(new URI(s"$hdfsBase$originalGraphPath"))
job.addCacheFile(new URI(s"$hdfsBase$perturbedGraphPath"))
```

#### Mapper Class - SimilarityMapper

The `SimilarityMapper` class serves the purpose of computing the similarity between a pair of nodes - one from the original graph and the other from the perturbed graph. The similarity computation is done locally on each mapper.

Upon initialization:

1. It reads the original and perturbed graphs from the distributed cache, using the `loadGraphFromHDFS` method.
2. It then processes each pair of nodes, and for each pair:
    - It calculates the similarity score using the `jaccardSimilarity` method from the `SimRank` class.
    - If the similarity score surpasses a specified threshold, the pair and its similarity score are written to the context as output. This is done to avoid flooding the reducer with insignificant results.

#### Reducer Class - SimilarityReducer

The `SimilarityReducer` class takes pairs of nodes (keys) and their associated similarity scores (values). For each pair, it determines the highest similarity score and writes this maximum value to the output.

The computation of the maximum similarity score is done in a recursive manner with the `getMaxSimilarity` method.

#### Similarity Computation - SimRank Class

The `SimRank` class contains methods for computing node similarity. The chosen similarity metric is the Jaccard similarity, which measures similarity between two sets. Here, these sets are subgraphs centered on the two nodes being compared.

Steps:

1. **Local Subgraph Extraction**: For both nodes, their respective local subgraphs up to a specified depth are extracted using `extractLocalSubgraph`.
2. **Jaccard Similarity Computation**: The Jaccard similarity between the two subgraphs is computed using the formula:

\[ \text{Similarity} = \frac{|\text{intersection of nodes}|}{|\text{union of nodes}|} \]

#### Output Format

The output data is written in a `<key, value>` format where:

- **Key**: The pair of nodes being compared, e.g., `<node1, node2>`
- **Value**: The Jaccard similarity score.

#### Questions for Documentation:

--------------------------------------------------------------------------
--------------------------------------------------------------------------
--------------------------------------------------------------------------
--------------------------------------------------------------------------
--------------------------------------------------------------------------
--------------------------------------------------------------------------



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

For a detailed walkthrough, watch our [demo video](https://vimeo.com/873263825?share=copy#t=0). In Depth documentation is on the way. 

## Limitations

1. **Memory Consumption**: Due to JVM heap size limits (`-Xmx2G`), datasets that require substantial memory might face issues.
2. **Single Threshold Level**: The application uses a single `similarityThreshold`, which may not be optimal for all use-cases.
3. **Unreliable Algorithm**
4. **Cloud and LocalHost versions have not been combined** 

**Note**: If you need more details or face any issues setting up, feel free to reach out. I'am here to help!
