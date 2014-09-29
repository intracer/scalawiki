import sbt.Keys._
import sbt._

object ScalaProjectBuild extends Build {

  lazy val scalaProject = Project(
    id = "mwbot",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "mwbot",
      organization := "org.intracer",
      version := "0.1.1",
      scalaVersion := "2.10.4",
      // add other settings here
        resolvers := Seq ("spray repo" at "http://repo.spray.io", "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"),
      libraryDependencies ++= {
        val akkaV = "2.3.2"
        val sprayV = "1.3.1"
        Seq(
          "io.spray" % "spray-client" % sprayV withSources() withJavadoc(),
          "io.spray" % "spray-caching" % sprayV withSources() withJavadoc(),
          "io.spray" %%  "spray-json" % "1.3.0" withSources() withJavadoc(),
          "com.typesafe.play" %% "play-json" % "2.3.4" withSources(),
          "com.typesafe.akka"   %%  "akka-actor"    % akkaV withSources() withJavadoc(),
          "com.typesafe.slick" %% "slick" % "2.1.0" withSources() withJavadoc(),
          "com.h2database" % "h2" % "1.3.175" withSources() withJavadoc(),
          "org.scala-lang" %% "scala-pickling" % "0.8.0" withSources() withJavadoc(),
          "com.github.tototoshi" %% "scala-csv" % "1.0.0",
          "com.rockymadden.stringmetric" %% "stringmetric-core" % "0.27.2",
          "no.priv.garshol.duke" %% "duke" % "1.2",
          "org.specs2" %% "specs2" % "2.3.12" % "test" withSources() withJavadoc()
        )
      },
//      publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))
        publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.ivy2/local")))
    )
  )


  scalacOptions in Test ++= Seq("-Yrangepos")
}
