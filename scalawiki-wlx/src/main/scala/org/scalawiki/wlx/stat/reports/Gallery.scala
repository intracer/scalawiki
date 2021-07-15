package org.scalawiki.wlx.stat.reports

import org.scalawiki.MwBot
import org.scalawiki.wlx.{ImageDB, MonumentDB}

import scala.util.Try

object Gallery {
  lazy val commons = MwBot.fromHost(MwBot.commons)

  def gallery(imageDb: ImageDB, mDb: MonumentDB): Unit = {

    val picturesPerMonument = imageDb.images.flatMap(_.monumentIds).groupBy(identity).mapValues(_.size).toMap
    val moderatelyPictured = picturesPerMonument.filter { case (_, size) => 4 <= size && size <= 9 }.keySet
    val byReg1 = moderatelyPictured.groupBy(_.split("-").head)
    for (reg1 <- byReg1.keySet.toSeq.sorted) {
      val reg1Ids = byReg1(reg1)
      val byReg2 = reg1Ids.groupBy(_.split("-")(1))
      for (reg2 <- byReg2.keySet.toSeq.sorted) {
        val reg2Ids = byReg2(reg2)
        val texts = for (monumentId <- reg2Ids.toSeq.sorted) yield {
          Try {
            val m = mDb.byId(monumentId).get
            s"""\n== $monumentId ${m.name} ==
               |* Дата ${m.year.getOrElse("")}
               |* Адреса: ${m.place.getOrElse("")}
               |* Населений пункт: ${m.cityName}
               |${m.gallery.map(g => s"* Категорія: [[:Category:$g|$g]]").getOrElse("")}
               |${m.article.map(a => s"* Стаття: [[:uk:$a|$a]]").getOrElse("")}""".stripMargin +
              imageDb.byId(monumentId).map(_.title).sorted.mkString("\n<gallery>\n", "\n", "\n</gallery>\n")
          }.getOrElse("")
        }
        val text = texts.mkString
        commons.page(s"Commons:WLM-UA/$reg1/$reg2").edit(text)
      }
    }
  }
}
