logLevel := Level.Warn

resolvers += Classpaths.sbtPluginReleases

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.9.2")

addSbtPlugin("com.typesafe.play" % "sbt-twirl" % "1.5.2")

addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.6.1")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "1.2.0")