name := "crawler"

version := "1.0"

scalaVersion := "2.12.2"

// HTTP wrapper library
libraryDependencies +=  "org.scalaj" %% "scalaj-http" % "2.3.0"
libraryDependencies += "org.jsoup" % "jsoup" % "1.10.2"

// for test
libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.1"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"