import sbt.Keys._
import sbt._

object ScalaProjectBuild extends Build {

  lazy val scalaProject = Project(
    id = "mwbot",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "mwbot",
      organization := "org.intracer",
      version := "0.2.1",
      scalaVersion := "2.10.4",
      // add other settings here
      resolvers := Seq("spray repo" at "http://repo.spray.io",
        "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"),
      libraryDependencies ++= {
        val akkaV = "2.3.4"
        val sprayV = "1.3.2"
        Seq(
          "io.spray" %% "spray-client" % sprayV withSources() withJavadoc(),
          "io.spray" %% "spray-caching" % sprayV withSources() withJavadoc(),
          "io.spray" %% "spray-json" % "1.3.0" withSources() withJavadoc(),
          "com.typesafe.play" %% "play-json" % "2.3.7" withSources(),
          "com.typesafe.akka" %% "akka-actor" % akkaV withSources() withJavadoc(),
          "com.typesafe.slick" %% "slick" % "2.1.0" withSources() withJavadoc(),
          "com.h2database" % "h2" % "1.3.175" withSources() withJavadoc(),
          "com.github.wookietreiber" %% "scala-chart" % "0.4.2" withSources() withJavadoc(),
          "org.jfree" % "jfreesvg" % "2.1" withSources() withJavadoc(),
      //    "org.scala-lang" %% "scala-pickling" % "0.9.0" withSources() withJavadoc(),
          "com.github.tototoshi" %% "scala-csv" % "1.0.0",
          //  "org.sweble.wikitext" % "sweble-wikitext" % "2.0.0",
          "org.sweble.wikitext" % "swc-engine" % "2.0.0",
          //          "org.sweble.engine" % "sweble-engine-serialization" % "2.0.0",
          "org.sweble.wom3" % "sweble-wom3-swc-adapter" % "2.0.0",
          "org.scalesxml" %% "scales-xml" % "0.5.0",
          "eu.cdevreeze.yaidom" %% "yaidom" % "1.3.2",

          "com.squants" %% "squants" % "0.4.2",

          "com.google.gdata" % "core" % "1.47.1",
          "com.google.api-client" % "google-api-client" % "1.19.1" withSources() withJavadoc() exclude("com.google.guava", "guava-jdk5"),
          "com.google.apis" % "google-api-services-drive" % "v2-rev155-1.19.1" withSources() withJavadoc(),
          "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2.1",

          "org.jopendocument" % "jOpenDocument" % "1.3" withSources() withJavadoc(),
          "org.apache.commons" % "commons-compress" % "1.9",
          "org.tukaani" % "xz" % "1.5",

          "org.eclipse.jgit" % "org.eclipse.jgit" % "3.6.2.201501210735-r" withSources() withJavadoc(),
          "jp.skypencil.java-diff-utils" % "diffutils" % "1.5.0" withSources() withJavadoc(),
          "com.sksamuel.elastic4s" %% "elastic4s" % "1.4.11" withSources() withJavadoc(),
          "org.apache.lucene" % "lucene-expressions" % "4.10.3" withSources() withJavadoc(),


        //          "nl.grons" %% "metrics-scala" % "3.3.0_a2.3",
          "com.rockymadden.stringmetric" %% "stringmetric-core" % "0.27.2",
          // "no.priv.garshol.duke" %% "duke" % "1.2",
          "org.specs2" %% "specs2" % "2.3.12" % "test" withSources() withJavadoc(),
           "com.google.jimfs" % "jimfs" % "1.0" % "test" withSources() withJavadoc(),
          "com.google.guava" % "guava" % "16.0.1" withSources() withJavadoc()

        )
      },
      publishTo := Some(Resolver.file("file", new File(Path.userHome.absolutePath + "/.m2/repository")))
      // publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.ivy2/local"))),

    ) ++ net.virtualvoid.sbt.graph.Plugin.graphSettings
  )


  scalacOptions in Test ++= Seq("-Yrangepos")

  conflictManager := ConflictManager.latestRevision
}
