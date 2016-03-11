name := "scalawiki"

organization := "org.scalawiki"

version := "0.4-M3"

scalaVersion := "2.11.8"

resolvers := Seq("spray repo" at "http://repo.spray.io",
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases",
  Resolver.jcenterRepo,
  Resolver.bintrayRepo("rick-beton", "maven")
)

libraryDependencies ++= {
  val akkaV = "2.3.14"
  val sprayV = "1.3.3"
  Seq(
    "io.spray" %% "spray-client" % sprayV,
    "io.spray" %% "spray-caching" % sprayV,
    "com.typesafe.play" %% "play-json" % "2.4.3",
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe" % "config" % "1.3.0",
    "com.iheart" %% "ficus" % "1.2.3",
    "commons-codec" % "commons-codec" % "1.10",
    "com.github.nscala-time" %% "nscala-time" % "2.10.0",
    "org.xwiki.commons" % "xwiki-commons-blame-api" % "6.4.1",
    "com.typesafe.slick" %% "slick" % "3.1.1",
    "com.typesafe.slick" %% "slick-hikaricp" % "3.1.1",
    //  "com.zaxxer" % "HikariCP" % "2.4.1",
    "com.h2database" % "h2" % "1.4.189",
    "com.github.wookietreiber" %% "scala-chart" % "0.5.0",
    //    "org.jfree" % "jfreesvg" % "2.1",
    "com.fasterxml" % "aalto-xml" % "1.0.0",
    //    "org.codehaus.woodstox" % "woodstox-core-asl" % "4.4.1",
    "org.apache.commons" % "commons-compress" % "1.9",
    "ch.qos.logback" % "logback-classic" % "1.1.3",
    "org.sweble.wikitext" % "swc-engine" % "2.0.0",
    "org.jsoup" % "jsoup" % "1.8.2",
    "com.github.tototoshi" %% "scala-csv" % "1.2.2",
    "uk.co.bigbeeconsultants" %% "bee-client" % "0.29.1",
    //    "org.apache.poi" % "poi-scratchpad" % "3.13",
    //    "org.apache.poi" % "poi-ooxml" % "3.13",

    "org.specs2" %% "specs2-core" % "3.6.4" % "test",
    "org.specs2" %% "specs2-matcher-extra" % "3.6.4" % "test",
    "com.google.jimfs" % "jimfs" % "1.0" % "test"
  )
}

scalacOptions ++= Seq("-Ybackend:GenBCode")

initialize := {
  val _ = initialize.value // run the previous initialization
  val required = VersionNumber("1.8")
  val curr = VersionNumber(sys.props("java.specification.version"))
  assert(CompatibleJavaVersion(curr, required), s"Java $required or above required")
}



    