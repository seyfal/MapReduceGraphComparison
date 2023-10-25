![Static Badge](https://img.shields.io/badge/build-passing-brightgreen)
![Version](https://img.shields.io/badge/version-1.0.0-blue)
![Scala Version](https://img.shields.io/badge/Scala-3.2.2-red)
![ScalaTest Version](https://img.shields.io/badge/ScalaTest-3.2.15-orange)
![Typesafe Config Version](https://img.shields.io/badge/Typesafe%20Config-1.4.2-brightgreen)
![Logback Version](https://img.shields.io/badge/Logback-1.4.7-yellow)
![SLF4J Version](https://img.shields.io/badge/SLF4J-2.0.5-lightgrey)
![Hadoop Client Version](https://img.shields.io/badge/Hadoop%20Client-3.3.6-blueviolet)
![License](https://img.shields.io/badge/license-MIT-green)


# Distributed Graph Comparison: 

## Table of Contents
1. [Introduction](#introduction)
2. [Quick Start](#quick-setup-guide)
3. [Video Walkthru](#video)
4. [System Architecture](#system-architecture)
5. [Code Logic and Flow](#code-logic-and-flow)
   - [Initialization](#initialization)
   - [Data Loading and Pre-processing](#data-loading-and-pre-processing)
   - [Distributed Matching](#distributed-matching)
   - [Post-processing](#post-processing)
   - [Result Compilation](#result-compilation)
6. [Generated Statistics](#generated-statistics)
   - [Overview](#overview)
   - [Matching Metrics](#matching-metrics)
   - [Performance Metrics](#performance-metrics)
   - [Error Metrics](#error-metrics)
7. [Areas of Improvement](#areas-of-improvement)
8. [Known Bugs and Issues](#known-bugs-and-issues)
9. [References and Citations](#references-and-citations)


## Introduction
The primary objective of this project is to leverage cloud-based frameworks and distributed computing technologies to analyze large-scale graph matching challenges. Using the [network simulator, NetGraphSim](https://github.com/0x1DOCD00D/NetGameSim), the application generates expansive graphs representative of big data. These graphs undergo specialized perturbation operations to produce their modified counterparts. The key challenge lies in discerning the differences between the original and perturbed graphs. 

This project not only aims to determine these differences but also seeks to quantify the matching accuracy through statistically robust methods. The results not only offer a holistic matching score for the graphs but also provide insights into how closely individual nodes and edges correlate. While the graph isomorphism problem is NP-complete, the approximation algorithms attempt to solve real-world challenges by comparing nodes and edges, producing meaningful metrics to gauge similarity.

## Quick Setup Guide
1. **Clone the Repository:** 
   ```
   git clone https://github.com/seyfal/MapReduceGraphComparison.git
   ```

2. **Navigate to Project Directory:**
   ```
   cd MapReduceGraphComparison
   ```

3. **Install Dependencies:** 
   Make sure you have SBT (Scala Build Tool) installed.
   ```
   sbt clean compile
   ```

4. **Configure HDFS Settings:**
   Modify the `application.conf` file to point to your HDFS installation and directory structures.

5. **Run the Application:** 
   Use SBT's run command. Adjust parameters as needed.
   ```
   sbt run
   ```

6. For further configurations and detailed setup, refer to the main documentation.



## Video

For a detailed walkthrough, watch our [demo video](https://vimeo.com/873263825?share=copy#t=0). 

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

```conf
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

$$ Similarity = {intersection-of-nodes \over union-of-nodes}$$

#### Output Format

The output data is written in a `<key, value>` format where:

- **Key**: The pair of nodes being compared, e.g., `<node1, node2>`
- **Value**: The Jaccard similarity score.

### Post-processing

After the MapReduce operation has completed its job, it is crucial to process the generated data to gain actionable insights. The post-processing phase consists of:

1. **Output Retrieval**: The first step is to fetch the output of the MapReduce job. This is done using the `loadOutputFile` function, which fetches the data from HDFS. The results are loaded into a map structure which has node pairs as keys and similarity scores as values. This map provides insights about the similarity between nodes from two different versions of the graph.

2. **Node Analysis**: Next, we extract various subsets of nodes from the output:
   - Same Nodes: These are nodes that remained unchanged between the two versions.
   - Modified Nodes: Nodes that have undergone changes.
   
   The above subsets are derived based on the similarity scores. A score of `1.0` indicates that the nodes are the same, while scores less than `1.0` indicate modifications.

3. **Performance Benchmarks**: 
   - Traceability links (TL) are calculated, including:
     - Good Traceability Links (GTL): Correct matches identified by the algorithm.
     - Bad Traceability Links (BTL): Errors in the algorithm's matching.

   Detailed metrics like `ACC`, `VPR`, and `BTLR` are computed. These metrics give insights into the accuracy, precision, and error rate of the algorithm.

### Result Compilation

Once post-processing is complete, the program provides the final statistics. These results are crucial for understanding how the algorithm performed. This phase includes:

1. **Printing Intermediate Statistics**: The script provides a comprehensive list of metrics such as `ATL`, `DTL`, `WTL`, `CTL`, `RTL`, `GTL`, and `BTL`. These metrics offer deep insights into the matching process.

2. **Final Metrics Calculation**: Advanced metrics like `ACC`, `VPR`, and `BTLR` are computed, providing a more holistic understanding of the algorithm's performance.

3. **Results Explanation**: The `explainResults` function gives a user-friendly explanation of the metrics. This feature is especially useful for those unfamiliar with graph processing, as it translates complex metrics into understandable language.

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

## Generated Statistics

### Overview

The core purpose of the algorithm is to compare and match components between the original graph and its perturbed version. The generated statistics shed light on the algorithm's efficiency, accuracy, and areas of potential improvement.

### Matching Metrics

1. **Traceability Links (TL)**: Represents a match between components (nodes or edges) of the original graph and its altered version.
2. **Good Traceability Links (GTL)**: Correct matches identified by the algorithm.
3. **Bad Traceability Links (BTL)**: Errors in the algorithm's matching.

### Performance Metrics

1. **Accuracy (ACC)**: Fraction of matches the algorithm correctly identified.
2. **Validation Precision Rate (VPR)**: Reflects the quality of matches.
3. **Bad Traceability Links Rate (BTLR)**: Fraction of total matches that were errors.

### Error Metrics

1. **Wrong Traceability Links (WTL)**: Represents mismatches made by the algorithm.

## Areas of Improvement
### 1. Algorithm Robustness
Current implementation provides a decent starting point for graph comparison and matching, but there's room for more complex and accurate algorithms. Exploring state-of-the-art methodologies can improve both accuracy and efficiency, catering to a broader range of use cases.

### 2. Advanced Data Storage in HDFS
With growing data sizes, there's a need to reconsider our current storage strategy in HDFS. Here are some ideas to explore:

- **Partitioned Storage:** By breaking the graph data into smaller partitions or blocks, there is a way to optimize read/write speeds and also ensure data redundancy.
  
- **Compression:** Employing compression techniques can reduce the storage space and potentially speed up data transfer times. 
  
- **Hybrid Cloud-Local Storage:** As mentioned, a system where both cloud and local storage solutions coexist can be invaluable. The goal would be to store frequently accessed or crucial data locally and push less critical or bulky data to the cloud.

### 3. Unified Version for Cloud and Local 
Application is currently split into separate versions for local and cloud environments. A unified version, capable of recognizing and adapting to its runtime environment, would streamline the deployment process and reduce maintenance efforts.

### 4. Support for .ngs Files from NetGameSim
Direct support for `.ngs` files will allow users to easily integrate with the NetGameSim system, bypassing manual conversions or extractions. This would involve building a dedicated parser for these files and ensuring compatibility with our existing structures.

## Known Bugs and Issues
- **Dual Versions:** As highlighted above, the project lacks a singular version running both on cloud and locally.

- **Limited File Support:** The system doesn't support `.ngs` files from NetGameSim directly. A converter or direct parser needs to be implemented to bridge this gap.


## References and Citations
- **NetGameSim Project:** Owned by Professor Mark Grechanik, this project provides the foundational simulation structures and tools. [Link](https://github.com/0x1DOCD00D/NetGameSim)
  
- **CS441_Fall2023 Homework Assignment:** The main task description and requirements that led to this project. [Link](https://github.com/0x1DOCD00D/CS441_Fall2023)
   
