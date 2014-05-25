import sbt._
import sbt.Keys._

object ScalaProjectBuild extends Build {

  lazy val scalaProject = Project(
    id = "client",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "client",
      organization := "org.intracer",
      version := "0.1",
      scalaVersion := "2.10.3",
      // add other settings here
        resolvers := Seq ("spray repo" at "http://repo.spray.io", "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"),
      libraryDependencies ++= {
        val akkaV = "2.2.3"
        val sprayV = "1.2.0"
        Seq(
          "io.spray" % "spray-client" % sprayV,
          "io.spray" %%  "spray-json" % "1.2.6",
          "com.typesafe.play" %% "play-json" % "2.2.3",
          "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
          "org.specs2" %% "specs2" % "2.3.12" % "test"
        )
      }
    )
  )

  scalacOptions in Test ++= Seq("-Yrangepos")
}
