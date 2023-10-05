import sbt.Keys.libraryDependencies

// Define the version of your project.
ThisBuild / version := "0.1.0-SNAPSHOT"

// Define the Scala version to be used.
ThisBuild / scalaVersion := "3.3.1"

// Define the project name.
name := "SimRank"

// Define library versions. Adjust the version numbers according to your needs.
val scalaTestVersion = "3.2.15"
val typeSafeConfigVersion = "1.4.2"
val logbackVersion = "1.4.7"
val sfl4sVersion = "2.0.5"
val graphVizVersion = "0.18.1"

// Define common dependencies shared across your project.
lazy val commonDependencies = Seq(
  "com.typesafe" % "config" % typeSafeConfigVersion, // Typesafe Config Library
  "ch.qos.logback" % "logback-classic" % logbackVersion, // Logback Classic Logger
  "org.slf4j" % "slf4j-api" % sfl4sVersion, // SLF4J API Module
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test, // ScalaTest for testing
  "io.circe" %% "circe-core" % "0.14.5", 
  "io.circe" %% "circe-generic" % "0.14.5", 
  "io.circe" %% "circe-yaml" % "0.14.2" 
)

// Define your project and its dependencies.
lazy val root = (project in file("."))
  .settings(
    scalaVersion := "3.3.1",
    name := "SimRank",
    libraryDependencies ++= commonDependencies // Adding common dependencies to your project
  )

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.4.7",
  "com.typesafe" % "config" % "1.4.2",
  "org.slf4j" % "slf4j-api" % "2.0.5",
  "org.apache.hadoop" % "hadoop-client" % "3.3.6",
  "org.yaml" % "snakeyaml" % "2.0"
)

// Define Scala Compiler options.
scalacOptions ++= Seq(
  "-deprecation", // Emit warning and location for usages of deprecated APIs
  "--explain-types", // Explain type errors in more detail
  "-feature" // Emit warning and location for usages of features that should be imported explicitly
)

// Define JVM options for running your project.
run / javaOptions ++= Seq(
  "-Xms512M", // Initial JVM heap size
  "-Xmx2G", // Maximum JVM heap size
  "-XX:+UseG1GC" // Use G1 Garbage Collector
)

// Define the main class. Replace with the actual main class of your application.
Compile / mainClass := Some("com.lsc.Main")
run / mainClass := Some("com.lsc.Main")


