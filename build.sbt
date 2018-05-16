import sbt.Keys._

val AkkaV = "2.5.11"
val AkkaHttpV = "10.0.13"
val PlayJsonV = "2.6.9"
val SpecsV = "3.9.5"
val TwirlV = "1.3.13"

fork in Test in ThisBuild := true

lazy val commonSettings = Seq(
  organization := "org.scalawiki",
  version := "0.5-M7",
  scalaVersion := "2.12.6",
  crossScalaVersions := Seq("2.12.6", "2.11.12"),
  conflictManager := ConflictManager.strict,

  libraryDependencies ++= Seq(
    "org.specs2" %% "specs2-core" % SpecsV % Test,
    "org.specs2" %% "specs2-matcher-extra" % SpecsV % Test,
    "org.specs2" %% "specs2-mock" % SpecsV % Test,
    "com.google.jimfs" % "jimfs" % "1.1" % Test,
    "com.github.tomakehurst" % "wiremock" % "2.16.0" % Test
  ),

  dependencyOverrides ++= {
    Seq(
      "com.typesafe.akka" %% "akka-actor" % AkkaV,
      "org.reactivestreams" % "reactive-streams" % "1.0.2",
      "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
      "com.google.guava" % "guava" % "20.0",
      "org.slf4j" % "slf4j-api" % "1.7.25",
      "commons-codec" % "commons-codec" % "1.10",
      "org.apache.commons" % "commons-lang3" % "3.6",
      "com.typesafe" % "config" % "1.3.2",
      "org.apache.poi" % "poi-ooxml" % "3.13"
    )
  },

  resolvers := Seq(
    "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
    "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases",
    Resolver.jcenterRepo,
    Resolver.bintrayRepo("rick-beton", "maven"),
    Resolver.bintrayRepo("softprops", "maven")
  ),

  initialize := {
    val _ = initialize.value
    // run the previous initialization
    val required = VersionNumber("1.8")
    val curr = VersionNumber(sys.props("java.specification.version"))
    assert(CompatibleJavaVersion(curr, required), s"Java $required or above required")
  }
)

lazy val scalawiki =
  (project in file("."))
    .settings(commonSettings)
    .dependsOn(
      `scalawiki-core`, `scalawiki-bots`, `scalawiki-dumps`, `scalawiki-wlx`, `scalawiki-sql`,
      `http-extensions`)
    .aggregate(
      `scalawiki-core`, `scalawiki-bots`, `scalawiki-dumps`, `scalawiki-wlx`, `scalawiki-sql`,
      `http-extensions`)

lazy val `scalawiki-core` =
  (project in file("scalawiki-core"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= {
      Seq(
        //        "io.spray" %% "spray-util" % SprayV,
        // https://mvnrepository.com/artifact/com.typesafe.akka/akka-http-caching
        "com.typesafe.akka" %% "akka-http-caching" % AkkaHttpV,
        "com.typesafe.play" %% "play-json" % PlayJsonV,
        "com.typesafe.akka" %% "akka-actor" % AkkaV,
        "com.typesafe.akka" %% "akka-stream" % AkkaV,
        "com.typesafe.akka" %% "akka-http" % AkkaHttpV,
        "com.typesafe" % "config" % "1.3.2",
        "com.iheart" %% "ficus" % "1.4.3",
        "jp.ne.opt" %% "chronoscala" % "0.1.5",
        "ch.qos.logback" % "logback-classic" % "1.1.3",
        "org.sweble.wikitext" % "swc-engine" % "3.1.7" exclude("org.jsoup", "jsoup"),
        "commons-codec" % "commons-codec" % "1.10",
        "org.jsoup" % "jsoup" % "1.8.3",
        "com.softwaremill.retry" %% "retry" % "0.3.0")
    }).dependsOn(`http-extensions`)

lazy val `scalawiki-bots` =
  (project in file("scalawiki-bots"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.github.pathikrit" %% "better-files-akka" % "3.4.0",
      "com.concurrentthought.cla" %% "command-line-arguments" % "0.5.0",
      "org.xwiki.commons" % "xwiki-commons-blame-api" % "6.4.1",
      "org.apache.poi" % "poi-scratchpad" % "3.13",
      "org.apache.poi" % "poi-ooxml" % "3.13",
      "fr.opensagres.xdocreport" % "org.apache.poi.xwpf.converter.xhtml" % "1.0.6",
      "com.typesafe.play" %% "twirl-api" % TwirlV,
      "com.github.tototoshi" %% "scala-csv" % "1.3.4"
    ))
    .dependsOn(`scalawiki-core` % "compile->compile;test->test", `scalawiki-wlx`)
    .enablePlugins(SbtTwirl)

lazy val `scalawiki-dumps` =
  (project in file("scalawiki-dumps"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++=
      Seq("com.fasterxml" % "aalto-xml" % "1.0.0",
        //    "org.codehaus.woodstox" % "woodstox-core-asl" % "4.4.1",
        "org.apache.commons" % "commons-compress" % "1.9")
    )
    .dependsOn(`scalawiki-core` % "compile->compile;test->test")

lazy val `scalawiki-wlx` =
  (project in file("scalawiki-wlx"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.github.wookietreiber" %% "scala-chart" % "0.5.1",
      "com.concurrentthought.cla" %% "command-line-arguments" % "0.5.0"
    ))
    .dependsOn(`scalawiki-core` % "compile->compile;test->test")

lazy val `scalawiki-sql` =
  (project in file("scalawiki-sql"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.typesafe.slick" %% "slick" % "3.2.3",
      "com.typesafe.slick" %% "slick-hikaricp" % "3.2.3",
      "com.h2database" % "h2" % "1.4.189"
    ))
    .dependsOn(`scalawiki-core` % "compile->compile;test->test")

lazy val `http-extensions` =
  (project in file("http-extensions"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % AkkaHttpV,
      "com.typesafe.akka" %% "akka-stream" % AkkaV,
      "com.typesafe.akka" %% "akka-actor" % AkkaV,
      "com.typesafe.play" %% "twirl-api" % TwirlV,
      "org.scalacheck" %% "scalacheck" % "1.14.0" % Test
    ))