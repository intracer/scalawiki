package org.scalawiki.wlx.stat

import org.scalawiki.MwBot
import org.scalawiki.cache.CachedBot
import org.scalawiki.dto.{Image, Namespace, Site}
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.Query
import org.scalawiki.query.QueryLibrary

import scala.concurrent.ExecutionContext.Implicits.global

object NominatedForDeletion extends QueryLibrary {

  def report(stat: ContestStat) = {
    val action = Action(Query(generatorWithTemplate("Delete", Set(Namespace.FILE))))

    val bot = new CachedBot(Site.commons, "delete", true)
    bot.run(action).map { nominated =>
      println("all nominated:" + nominated.size)

      stat.totalImageDb.map { db =>
        val wlmIds = db.images.flatMap(_.pageId).toSet
        val nominatedIds = nominated.flatMap(_.id).toSet
        val wlmNominatedIds = wlmIds.intersect(nominatedIds)
        println("wlm nominated:" + wlmNominatedIds.size)

        val images = db.images.filter(_.pageId.exists(wlmNominatedIds.contains)).map(_.title).sorted
        val gallery = Image.gallery(images)
        MwBot.fromHost(MwBot.commons)
          .page("Commons:Wiki Loves Monuments in Ukraine nominated for deletion")
          .edit(gallery)
      }
    }
  }
}
