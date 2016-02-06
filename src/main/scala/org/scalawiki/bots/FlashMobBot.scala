package org.scalawiki.bots

import org.scalawiki.MwBot
import org.scalawiki.dto.Namespace
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.prop.{Links, PlNamespace, Prop}
import org.scalawiki.dto.cmd.query.{Query, TitlesParam}

import scala.concurrent.ExecutionContext.Implicits.global

object FlashMobBot {

  def main(args: Array[String]) {
    val bot = MwBot.get(MwBot.ukWiki)
    val title = "Вікіпедія:Вікіфлешмоб 2016/Сувеніри"

    val whatLinksHere = Action(Query(
      Prop(Links(PlNamespace(Seq(Namespace.USER)))),
      TitlesParam(Seq(title))
    ))

    bot.run(whatLinksHere) map {
      pages =>

        val users = pages.head.links.map(_.title.split("\\:").last)

        println("Users:" + users.size)
        users.foreach(println)
    }
  }


}
