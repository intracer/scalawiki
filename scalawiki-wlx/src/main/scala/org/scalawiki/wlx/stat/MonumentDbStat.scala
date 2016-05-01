package org.scalawiki.wlx.stat

import java.text.NumberFormat

import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.MonumentDB

class MonumentDbStat {

  val format = NumberFormat.getPercentInstance

  val columns = Seq(
    "country", 	"lang", 	"total", 	"name", "address",	"coordinates", 	"image", 	"commonscat", 	"article"
  )

  def getStat(monumentDbs: Seq[MonumentDB]) = {
    val title = monumentDbs.head.contest.contestType.name + " database statistics"

    val data = monumentDbs.map { db =>
      val country = db.contest.country.code
      val lang = db.contest.listsHost
      val total = db.ids.size

      val monuments = db.monuments
      val name = monuments.count(_.name.nonEmpty)
      val address = monuments.count(_.place.isDefined)
      val coordinates = monuments.count(m => m.lat.isDefined && m.lon.isDefined)
      val image = monuments.count(_.photo.isDefined)
      val commonsCat = monuments.count(_.gallery.isDefined)
      val article = monuments.count(_.article.isDefined)

      def withPercentage(value: Int) =
        s"$value <small>(${format.format(value.toDouble / total.toDouble)})</small>"

      Seq(country, lang, total.toString,
        withPercentage(name),
        withPercentage(address),
        withPercentage(coordinates),
        withPercentage(image),
        withPercentage(commonsCat),
        withPercentage(article)
      )
    }

    val table = new Table(columns, data, title)

    table.asWiki
  }



}


