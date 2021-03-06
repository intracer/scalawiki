import sbt._

object Dependencies {

  val AaltoXmlV = "1.3.0"
  val BetterFilesV = "3.9.1"
  val BlameApiV = "13.5"
  val ChronicleMapV = "3.19.40"
  val ChronoScalaV = "1.0.0"
  val FicusV = "1.5.0"
  val GuavaV = "20.0"
  val H2V = "1.4.200"
  val JimFsV = "1.2"
  val JodaTimeV = "2.9.9"
  val JSoupV = "1.14.1"
  val LogbackClassicV = "1.2.3"
  val MockServerV = "5.7.0"
  val ReactiveStreamsV = "1.0.2"
  val RetryV = "0.3.3"
  val Scala213V = "2.13.6"
  val Scala212V = "2.12.14"
  val Scala211V = "2.11.12"
  val ScalaChartV = "0.7.1"
  val ScalaCheckV = "1.15.2"
  val ScalaCsvV = "1.3.8"
  val ScalaParserCombinatorsV = "1.1.2"
  val ScalaXmlV = "1.2.0"
  val ScallopV = "4.0.2"
  val Slf4jV = "1.7.25"
  val SwcEngineV = "3.1.9"
  val TypesafeConfigV = "1.4.1"

  val resolvers = Seq(
    "Typesafe Repo" at "https://repo.typesafe.com/typesafe/releases/",
    "Scalaz Bintray Repo" at "https://dl.bintray.com/scalaz/releases",
    Resolver.jcenterRepo,
    Resolver.bintrayRepo("rick-beton", "maven"),
    Resolver.bintrayRepo("softprops", "maven")
  )

  val overrides = Seq(
    Library.Akka.actor,
    "org.reactivestreams" % "reactive-streams" % ReactiveStreamsV,
    "org.scala-lang.modules" %% "scala-xml" % ScalaXmlV,
    "org.scala-lang.modules" %% "scala-parser-combinators" % ScalaParserCombinatorsV,
    "com.google.guava" % "guava" % GuavaV,
    Library.Commons.codec,
    Library.Commons.compress,
    Library.Commons.io,
    Library.Commons.lang,
    "com.typesafe" % "config" % TypesafeConfigV,
    Library.Poi.ooxml,
    Library.Jackson.core,
    Library.Jackson.annotations,
    Library.Jackson.databind,
    "joda-time" % "joda-time" % JodaTimeV,
    "org.slf4j" % "slf4j-api" % Slf4jV,
    "ch.qos.logback" % "logback-classic" % LogbackClassicV,
    "javax.xml.bind" % "jaxb-api" % "2.3.1"
  )

  object Library {

    object Akka {
      val AkkaV = "2.5.32"
      val AkkaHttpV = "10.1.14"

      val actor = "com.typesafe.akka" %% "akka-actor" % AkkaV
      val stream = "com.typesafe.akka" %% "akka-stream" % AkkaV
      val http = "com.typesafe.akka" %% "akka-http" % AkkaHttpV
      val httpCaching = "com.typesafe.akka" %% "akka-http-caching" % AkkaHttpV
    }

    object Play {
      def PlayJsonV(isScala213: Boolean) = {
        if (isScala213) {
          "2.7.4"
        } else {
          "2.6.14" // scala-steward:off
        }
      }

      def TwirlV(isScala213: Boolean) = "1.4.2"

      def json(isScala213: Boolean) = "com.typesafe.play" %% "play-json" % PlayJsonV(isScala213)

      def twirlApi(isScala213: Boolean) = "com.typesafe.play" %% "twirl-api" % TwirlV(isScala213)
    }

    object Poi {
      val PoiV = "5.0.0"
      val PoiXwpfV = "1.0.6"

      val scratchpad = "org.apache.poi" % "poi-scratchpad" % PoiV
      val ooxml = "org.apache.poi" % "poi-ooxml" % PoiV
      val converter = "fr.opensagres.xdocreport" % "org.apache.poi.xwpf.converter.xhtml" % PoiXwpfV
    }

    object Commons {
      val CommonsCodecV = "1.15"
      val CommonsCompressV = "1.21"
      val CommonsLang3V = "3.7"
      val CommonsIoV = "2.6"

      val codec = "commons-codec" % "commons-codec" % CommonsCodecV
      val io = "commons-io" % "commons-io" % CommonsIoV
      val lang = "org.apache.commons" % "commons-lang3" % CommonsLang3V
      val compress = "org.apache.commons" % "commons-compress" % CommonsCompressV
    }

    object Jackson {
      val JacksonV = "2.9.2"

      val core = "com.fasterxml.jackson.core" % "jackson-core" % JacksonV
      val annotations = "com.fasterxml.jackson.core" % "jackson-annotations" % JacksonV
      val databind = "com.fasterxml.jackson.core" % "jackson-databind" % JacksonV
    }

    object Slick {
      val SlickV = "3.3.3"

      val slick = "com.typesafe.slick" %% "slick" % SlickV
      val hikaricp = "com.typesafe.slick" %% "slick-hikaricp" % SlickV
    }

    object Specs2 {
      val SpecsV = "4.10.6"

      val core = "org.specs2" %% "specs2-core" % SpecsV
      val matcherExtra = "org.specs2" %% "specs2-matcher-extra" % SpecsV
      val mock = "org.specs2" %% "specs2-mock" % SpecsV
    }

  }

}