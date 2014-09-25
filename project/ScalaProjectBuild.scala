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
          "io.spray" % "spray-client" % sprayV,
          "io.spray" % "spray-caching" % sprayV,
          "io.spray" %%  "spray-json" % "1.3.0",
          "com.typesafe.play" %% "play-json" % "2.3.6",
          "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
          "com.typesafe.slick" %% "slick" % "2.1.0",
          "com.h2database" % "h2" % "1.3.175",
          "org.scala-lang" %% "scala-pickling" % "0.8.0",
          "org.specs2" %% "specs2" % "2.3.12" % "test"

        )
      },
//      publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))
        publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.ivy2/local")))
    )
  )


  scalacOptions in Test ++= Seq("-Yrangepos")
}
