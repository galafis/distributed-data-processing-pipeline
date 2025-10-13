name := "distributed-data-processing-pipeline"

version := "1.0.0"

scalaVersion := "2.12.18"

organization := "com.gabriellafis"

libraryDependencies ++= Seq(
  // Spark Core
  "org.apache.spark" %% "spark-core" % "3.5.0" % "provided",
  "org.apache.spark" %% "spark-sql" % "3.5.0" % "provided",
  "org.apache.spark" %% "spark-streaming" % "3.5.0" % "provided",
  
  // Delta Lake
  "io.delta" %% "delta-core" % "2.4.0",
  
  // Configuration
  "com.typesafe" % "config" % "1.4.3",
  
  // Logging
  "org.slf4j" % "slf4j-api" % "2.0.9",
  "ch.qos.logback" % "logback-classic" % "1.4.11",
  
  // JSON Processing
  "org.json4s" %% "json4s-jackson" % "4.0.6",
  
  // Testing
  "org.scalatest" %% "scalatest" % "3.2.17" % Test,
  "org.scalatestplus" %% "mockito-4-11" % "3.2.17.0" % Test
)

// Assembly settings for fat JAR
assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case "reference.conf" => MergeStrategy.concat
  case x => MergeStrategy.first
}

assembly / assemblyJarName := s"${name.value}-${version.value}.jar"

// Compiler options
scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlint"
)

// Test options
Test / parallelExecution := false
Test / fork := true

