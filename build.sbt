import sbt.Keys._
import Dependencies._

fork in Test in ThisBuild := true

lazy val commonSettings = Seq(
  organization := "org.scalawiki",
  version := "0.5.1-SNAPSHOT",
  scalaVersion := Scala212V,
  crossScalaVersions := Seq(Scala212V, Scala211V),
  conflictManager := ConflictManager.strict,
  licenses += ("Apache-2.0", url("http://opensource.org/licenses/Apache-2.0")),

  Keys.resolvers ++= Dependencies.resolvers,

  libraryDependencies ++= Seq(
    Library.Specs2.core,
    Library.Specs2.matcherExtra,
    Library.Specs2.mock,
    "com.google.jimfs" % "jimfs" % JimFsV % Test,
    "org.mock-server" % "mockserver-netty" % MockServerV % Test
  ),

  dependencyOverrides ++= Dependencies.overrides,

  initialize := {
    val _ = initialize.value
    val required = VersionNumber("1.8")
    val curr = VersionNumber(sys.props("java.specification.version"))
    assert(CompatibleJavaVersion(curr, required), s"Java $required or above required")
  }
)

lazy val scalawiki = (project in file("."))
  .settings(commonSettings)
  .dependsOn(
    `scalawiki-core`, `scalawiki-bots`, `scalawiki-dumps`, `scalawiki-wlx`, `scalawiki-sql`,
    `http-extensions`)
  .aggregate(
    `scalawiki-core`, `scalawiki-bots`, `scalawiki-dumps`, `scalawiki-wlx`, `scalawiki-sql`,
    `http-extensions`)

lazy val `scalawiki-core` = (project in file("scalawiki-core"))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= {
    Seq(
      Library.Akka.actor,
      Library.Akka.stream,
      Library.Akka.http,
      Library.Akka.httpCaching,
      Library.Play.json,
      "com.typesafe" % "config" % TypesafeConfigV,
      "com.iheart" %% "ficus" % FicusV,
      "jp.ne.opt" %% "chronoscala" % ChronoScalaV,
      "ch.qos.logback" % "logback-classic" % LogbackClassicV,
      "org.sweble.wikitext" % "swc-engine" % SwcEngineV exclude("org.jsoup", "jsoup"),
      Library.Commons.codec,
      "org.jsoup" % "jsoup" % JSoupV,
      "com.softwaremill.retry" %% "retry" % RetryV
    )
  }).dependsOn(`http-extensions`)

lazy val `scalawiki-bots` = (project in file("scalawiki-bots"))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= Seq(
    "com.github.pathikrit" %% "better-files-akka" % BetterFilesAkkaV,
    "com.concurrentthought.cla" %% "command-line-arguments" % CommandLineArgumentsV,
    "org.xwiki.commons" % "xwiki-commons-blame-api" % BlameApiV,
    Library.Poi.scratchpad,
    Library.Poi.ooxml,
    Library.Poi.converter,
    Library.Play.twirlApi,
    "com.github.tototoshi" %% "scala-csv" % ScalaCsvV
  ))
  .dependsOn(`scalawiki-core` % "compile->compile;test->test", `scalawiki-wlx`)
  .enablePlugins(SbtTwirl)

lazy val `scalawiki-dumps` = (project in file("scalawiki-dumps"))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++=
    Seq("com.fasterxml" % "aalto-xml" % AaltoXmlV,
      Library.Commons.compress
    ))
  .dependsOn(`scalawiki-core` % "compile->compile;test->test")

lazy val `scalawiki-wlx` = (project in file("scalawiki-wlx"))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= Seq(
    "com.github.wookietreiber" %% "scala-chart" % ScalaChartV,
    "com.concurrentthought.cla" %% "command-line-arguments" % CommandLineArgumentsV
  ))
  .dependsOn(`scalawiki-core` % "compile->compile;test->test")

lazy val `scalawiki-sql` = (project in file("scalawiki-sql"))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= Seq(
    Library.Slick.slick,
    Library.Slick.hikaricp,
    "com.h2database" % "h2" % H2V
  ))
  .dependsOn(`scalawiki-core` % "compile->compile;test->test")

lazy val `http-extensions` =
  (project in file("http-extensions"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq(
      Library.Akka.actor,
      Library.Akka.stream,
      Library.Akka.http,
      Library.Play.twirlApi,
      "org.scalacheck" %% "scalacheck" % ScalaCheckV % Test
    ))