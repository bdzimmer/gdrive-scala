// Copyright (c) 2015 Ben Zimmer. All rights reserved.

// gdrive-scala project sbt file

lazy val root = (project in file("."))
  .settings(
    name := "gdrive-scala",
    version := "2015.05.26",
    organization := "bdzimmer",
    scalaVersion := "2.10.5",
    
    javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),
    
    libraryDependencies ++= Seq(
      "commons-io" % "commons-io" %  "2.4",
      "com.google.apis" % "google-api-services-drive" % "v2-rev167-1.20.0"
    ))


    
// import into Eclipse as a Scala project
EclipseKeys.projectFlavor := EclipseProjectFlavor.Scala

// use Java 1.7 in Eclipse    
EclipseKeys.executionEnvironment := Some(EclipseExecutionEnvironment.JavaSE17)

// use the version of Scala from sbt in Eclipse
EclipseKeys.withBundledScalaContainers := false