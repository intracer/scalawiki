logLevel := Level.Warn

resolvers += Classpaths.sbtPluginReleases

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.1.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-twirl" % "1.5.1")

addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.6.1")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.2.0")
