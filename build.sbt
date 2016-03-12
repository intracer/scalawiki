import sbt.Keys._

lazy val scalawiki =
  (project in file("."))
    .settings(commonSettings)
    .dependsOn(
      `scalawiki-core`, `scalawiki-bots`, `scalawiki-dumps`, `scalawiki-wlx`, `scalawiki-sql`,
      `spray-cookies`
    )
    .aggregate(
      `scalawiki-core`, `scalawiki-bots`, `scalawiki-dumps`, `scalawiki-wlx`, `scalawiki-sql`,
      `spray-cookies`
    )

val akkaV = "2.3.14"
val sprayV = "1.3.3"

lazy val `scalawiki-core` =
  (project in file("scalawiki-core"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= {
      Seq(
        "io.spray" %% "spray-client" % sprayV,
        "io.spray" %% "spray-caching" % sprayV,
        "com.typesafe.play" %% "play-json" % "2.4.3",
        "com.typesafe.akka" %% "akka-actor" % akkaV,
        "com.typesafe" % "config" % "1.3.0",
        "com.iheart" %% "ficus" % "1.2.3",
        "com.github.nscala-time" %% "nscala-time" % "2.10.0",
        "org.xwiki.commons" % "xwiki-commons-blame-api" % "6.4.1",
        "ch.qos.logback" % "logback-classic" % "1.1.3",
        "com.github.tototoshi" %% "scala-csv" % "1.2.2",
        "uk.co.bigbeeconsultants" %% "bee-client" % "0.29.1",
        "org.sweble.wikitext" % "swc-engine" % "2.0.0",
        "commons-codec" % "commons-codec" % "1.10"
      )
    }).dependsOn(`spray-cookies`)

lazy val `scalawiki-bots` =
  (project in file("scalawiki-bots"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "org.jsoup" % "jsoup" % "1.8.2"
      //    "org.apache.poi" % "poi-scratchpad" % "3.13",
      //    "org.apache.poi" % "poi-ooxml" % "3.13",
    ))
    .dependsOn(`scalawiki-core` % "compile->compile;test->test", `scalawiki-wlx`)

lazy val `scalawiki-dumps` =
  (project in file("scalawiki-dumps"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++=
      Seq("com.fasterxml" % "aalto-xml" % "1.0.0",
        //    "org.codehaus.woodstox" % "woodstox-core-asl" % "4.4.1",
        "org.apache.commons" % "commons-compress" % "1.9")
    )
    .dependsOn(`scalawiki-core`)

lazy val `scalawiki-wlx` =
  (project in file("scalawiki-wlx"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq("com.github.wookietreiber" %% "scala-chart" % "0.5.0"))
    .dependsOn(`scalawiki-core`, `scalawiki-sql`)

lazy val `scalawiki-sql` =
  (project in file("scalawiki-sql"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.typesafe.slick" %% "slick" % "3.1.1",
      "com.typesafe.slick" %% "slick-hikaricp" % "3.1.1",
      "com.h2database" % "h2" % "1.4.189"
    ))
    .dependsOn(`scalawiki-core`)

lazy val `spray-cookies` =
  (project in file("spray-cookies"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "io.spray" %% "spray-client" % sprayV,
      "io.spray" %% "spray-json" % "1.2.6",
      "com.typesafe.akka" %% "akka-actor" % akkaV,
      "org.scalacheck" %% "scalacheck" % "1.11.3" % "test"
    ))

lazy val commonSettings = Seq(
  organization := "org.scalawiki",
  version := "0.4",
  scalaVersion := "2.11.8",

  libraryDependencies ++= Seq(
    "org.specs2" %% "specs2-core" % "3.6.4" % "test",
    "org.specs2" %% "specs2-matcher-extra" % "3.6.4" % "test",
    "com.google.jimfs" % "jimfs" % "1.0" % "test"
  ),

  resolvers := Seq("spray repo" at "http://repo.spray.io",
    "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
    "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases",
    Resolver.jcenterRepo,
    Resolver.bintrayRepo("rick-beton", "maven")
  ),
  scalacOptions ++= Seq("-Ybackend:GenBCode"),

  /**
    * For now the only reason to require Java 8 is
    * "com.typesafe.play" %% "play-json" % "2.4.3" dependency.
    * It is possible to implement crossbuild ([[https://github.com/intracer/scalawiki/issues/36 gh issue]])
    * for Java 7 with play-json 2.3
    */
  initialize := {
    val _ = initialize.value // run the previous initialization
    val required = VersionNumber("1.8")
    val curr = VersionNumber(sys.props("java.specification.version"))
    assert(CompatibleJavaVersion(curr, required), s"Java $required or above required")
  }
)




    