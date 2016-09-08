package org.scalawiki.bots.stat

import com.typesafe.config.{Config, ConfigFactory}
import org.joda.time.DateTime

case class ArticlesEvent(
                     name: String,
                     start: DateTime,
                     end: DateTime,
                     newTemplate: String,
                     improvedTemplate: String)

object Events {

  import net.ceedubs.ficus.Ficus._
  import org.scalawiki.time.imports._

  import scala.collection.JavaConverters._

  def fromConfig(c: Config) =
    new ArticlesEvent(
      c.getString("name"),
      c.as[DateTime]("start"),
      c.as[DateTime]("end"),
      c.getString("new-template"),
      c.getString("improved-template")
    )

  def events() = {
    val conf = ConfigFactory.load("articles-events.conf")
    val contests = conf.getConfigList("contests").asScala.map(fromConfig)
    val weeks = conf.getConfigList("weeks").asScala.map(fromConfig)
    (contests, weeks)
  }

  def main(args: Array[String]) {
    val (contests, weeks) = events()

    println(s"Contests: ${contests.mkString("\n")}")
    println(s"Weeks: ${weeks.mkString("\n")}")
  }
}