package org.scalawiki.wlx.stat

import java.text.NumberFormat

import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.MonumentDB

class MonumentDbStat {

  val format = NumberFormat.getPercentInstance

  def getStat(monumentDb: MonumentDB) = {
    val columns = Seq(
    "country", 	"lang", 	"total", 	"name", "address",	"coordinates", 	"image", 	"commonscat", 	"article"
    )

    val country = monumentDb.contest.country.code
    val lang = monumentDb.contest.country.languageCode
    val total = monumentDb.ids.size

    val monuments = monumentDb.monuments
    val name = monuments.count(_.name.nonEmpty)
    val address = monuments.count(_.place.isDefined)
    val coordinates = monuments.count(m => m.lat.isDefined && m.lon.isDefined)
    val image = monuments.count(_.photo.isDefined)
    val commonsCat = monuments.count(_.gallery.isDefined)
    val article = monuments.count(_.article.isDefined)

    def withPercentage(value: Int) =
      s"$value <small>(${format.format(value.toDouble / total.toDouble)})</small>"

    val data = Seq(country, lang, total.toString,
      withPercentage(name),
      withPercentage(address),
      withPercentage(coordinates),
      withPercentage(image),
      withPercentage(commonsCat),
      withPercentage(article)
    )

    val title = monumentDb.contest.contestType.name + " database statistics"
    val table = new Table(title, columns, Seq(data))

    table.asWiki
  }



}


