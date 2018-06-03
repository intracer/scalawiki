import sbt.Keys._
import Dependencies._

fork in Test in ThisBuild := true

lazy val commonSettings = Seq(
  organization := "org.scalawiki",
  version := "0.5-M7",
  scalaVersion := Scala212V,
  crossScalaVersions := Seq(Scala212V, Scala211V),
  conflictManager := ConflictManager.strict,
  licenses += ("Apache-2.0", url("http://opensource.org/licenses/Apache-2.0")),

  resolvers := Seq(
    "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
    "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases",
    Resolver.jcenterRepo,
    Resolver.bintrayRepo("rick-beton", "maven"),
    Resolver.bintrayRepo("softprops", "maven")
  ),

  libraryDependencies ++= Seq(
    "org.specs2" %% "specs2-core" % SpecsV % Test,
    "org.specs2" %% "specs2-matcher-extra" % SpecsV % Test,
    "org.specs2" %% "specs2-mock" % SpecsV % Test,
    "com.google.jimfs" % "jimfs" % JimFsV % Test,
    "org.mock-server" % "mockserver-netty" % MockServerV % Test
  ),

  dependencyOverrides ++= {
    Seq(
      "com.typesafe.akka" %% "akka-actor" % AkkaV,
      "org.reactivestreams" % "reactive-streams" % ReactiveStreamsV,
      "org.scala-lang.modules" %% "scala-xml" % ScalaXmlV,
      "com.google.guava" % "guava" % GuavaV,
      "org.slf4j" % "slf4j-api" % Slf4jV,
      "commons-codec" % "commons-codec" % CommonsCodecV,
      "commons-io" % "commons-io" % CommonsIoV,
      "org.apache.commons" % "commons-lang3" % CommonsLang3V,
      "com.typesafe" % "config" % TypesafeConfigV,
      "org.apache.poi" % "poi-ooxml" % PoiV,
      "com.fasterxml.jackson.core" % "jackson-core" % JacksonV,
      "com.fasterxml.jackson.core" % "jackson-annotations" % JacksonV,
      "com.fasterxml.jackson.core" % "jackson-databind" % JacksonV,
      "joda-time" % "joda-time" % JodaTimeV,
      "ch.qos.logback" % "logback-classic" % LogbackClassicV
    )
  },

  initialize := {
    val _ = initialize.value
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
        "com.typesafe.akka" %% "akka-http-caching" % AkkaHttpV,
        "com.typesafe.play" %% "play-json" % PlayJsonV,
        "com.typesafe.akka" %% "akka-actor" % AkkaV,
        "com.typesafe.akka" %% "akka-stream" % AkkaV,
        "com.typesafe.akka" %% "akka-http" % AkkaHttpV,
        "com.typesafe" % "config" % TypesafeConfigV,
        "com.iheart" %% "ficus" % FicusV,
        "jp.ne.opt" %% "chronoscala" % ChronoScalaV,
        "ch.qos.logback" % "logback-classic" % LogbackClassicV,
        "org.sweble.wikitext" % "swc-engine" % SwcEngineV exclude("org.jsoup", "jsoup"),
        "commons-codec" % "commons-codec" % CommonsCodecV,
        "org.jsoup" % "jsoup" % JSoupV,
        "com.softwaremill.retry" %% "retry" % RetryV
      )
    }).dependsOn(`http-extensions`)

lazy val `scalawiki-bots` =
  (project in file("scalawiki-bots"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.github.pathikrit" %% "better-files-akka" % BetterFilesAkkaV,
      "com.concurrentthought.cla" %% "command-line-arguments" % CommandLineArgumentsV,
      "org.xwiki.commons" % "xwiki-commons-blame-api" % BlameApiV,
      "org.apache.poi" % "poi-scratchpad" % PoiV,
      "org.apache.poi" % "poi-ooxml" % PoiV,
      "fr.opensagres.xdocreport" % "org.apache.poi.xwpf.converter.xhtml" % PoiXwpfV,
      "com.typesafe.play" %% "twirl-api" % TwirlV,
      "com.github.tototoshi" %% "scala-csv" % ScalaCsvV
    ))
    .dependsOn(`scalawiki-core` % "compile->compile;test->test", `scalawiki-wlx`)
    .enablePlugins(SbtTwirl)

lazy val `scalawiki-dumps` =
  (project in file("scalawiki-dumps"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++=
      Seq("com.fasterxml" % "aalto-xml" % AaltoXmlV,
        //    "org.codehaus.woodstox" % "woodstox-core-asl" % "4.4.1",
        "org.apache.commons" % "commons-compress" % CommonsCompressV
      ))
    .dependsOn(`scalawiki-core` % "compile->compile;test->test")

lazy val `scalawiki-wlx` =
  (project in file("scalawiki-wlx"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.github.wookietreiber" %% "scala-chart" % ScalaChartV,
      "com.concurrentthought.cla" %% "command-line-arguments" % CommandLineArgumentsV
    ))
    .dependsOn(`scalawiki-core` % "compile->compile;test->test")

lazy val `scalawiki-sql` =
  (project in file("scalawiki-sql"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "com.typesafe.slick" %% "slick" % SlickV,
      "com.typesafe.slick" %% "slick-hikaricp" % SlickV,
      "com.h2database" % "h2" % H2V
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
      "org.scalacheck" %% "scalacheck" % ScalaCheckV % Test
    ))