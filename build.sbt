name := "scalawiki"

organization := "org.scalawiki"

version := "0.3"

scalaVersion := "2.11.5"

resolvers := Seq("spray repo" at "http://repo.spray.io",
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/")

libraryDependencies ++= {
  val akkaV = "2.3.9"
  val sprayV = "1.3.2"
  Seq(
    "io.spray" %% "spray-client" % sprayV,
    "io.spray" %% "spray-caching" % sprayV,
    "com.typesafe.play" %% "play-json" % "2.3.7",
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "commons-codec" % "commons-codec" % "1.10",
    "com.github.nscala-time" %% "nscala-time" % "1.8.0",
    "org.xwiki.commons" % "xwiki-commons-blame-api" % "6.4.1",
    "com.typesafe.slick" %% "slick" % "2.1.0",
    "com.h2database" % "h2" % "1.3.175",
    "com.github.wookietreiber" %% "scala-chart" % "0.4.2",
    "org.jfree" % "jfreesvg" % "2.1",
    "org.specs2" %% "specs2" % "2.3.12" % "test")
}

    