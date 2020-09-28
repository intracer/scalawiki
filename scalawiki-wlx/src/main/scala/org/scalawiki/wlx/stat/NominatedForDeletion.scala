package org.scalawiki.wlx.stat

import org.scalawiki.MwBot
import org.scalawiki.cache.CachedBot
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.prop.iiprop.{IiProp, Url}
import org.scalawiki.dto.cmd.query.prop.rvprop.RvProp
import org.scalawiki.dto.cmd.query.prop.{ImageInfo, Prop, Revisions, rvprop}
import org.scalawiki.dto.cmd.query.{PageIdsParam, Query}
import org.scalawiki.dto.{Image, Namespace, Site}
import org.scalawiki.query.QueryLibrary
import org.scalawiki.wlx.ImageDB

import scala.concurrent.ExecutionContext.Implicits.global

object NominatedForDeletion extends QueryLibrary {
  val commons = MwBot.fromHost(MwBot.commons)

  def report(stat: ContestStat) = {
    val findNominatedAction = Action(Query(generatorWithTemplate("Delete", Set(Namespace.FILE))))

    val cachedBot = new CachedBot(Site.commons, "delete", true)
    cachedBot.run(findNominatedAction).map { nominated =>
      println("all nominated:" + nominated.size)

      stat.totalImageDb.map { db =>
        val wlmIds = db.images.flatMap(_.pageId).toSet
        val nominatedIds = nominated.flatMap(_.id).toSet
        val wlmNominatedIds = wlmIds.intersect(nominatedIds)
        println("wlm nominated:" + wlmNominatedIds.size)

        makeGallery(db, wlmNominatedIds)

        copyToWikipedia(wlmNominatedIds)
      }
    }
  }

  private def makeGallery(db: ImageDB, wlmNominatedIds: Set[Long]) = {
    val images = db.images.filter(_.pageId.exists(wlmNominatedIds.contains)).map(_.title).sorted
    val gallery = Image.gallery(images)
    commons.page("Commons:Wiki Loves Monuments in Ukraine nominated for deletion")
      .edit(gallery)
  }

  private def copyToWikipedia(wlmNominatedIds: Set[Long]) = {
    val nominatedInfoAction = Action(Query(
      PageIdsParam(wlmNominatedIds.toSeq),
      Prop(
        Revisions(Seq(RvProp(rvprop.Ids, rvprop.Content)): _*),
        ImageInfo(IiProp(Url))
      )))

    val ukWiki = MwBot.fromHost(MwBot.ukWiki)

    commons.run(nominatedInfoAction).map { nominatedInfos =>
      nominatedInfos.map { info =>
        val image = info.images.head
        for (text <- info.text;
             url <- image.url) {
          commons.getByteArray(url).map { bytes =>
            val title = info.title
            ukWiki.page(title).upload(title, bytes, info.text, Some("moving from commons"))
          }
        }
      }
    }
  }

}
