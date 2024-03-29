package org.scalawiki.wlx.stat.reports

import org.scalawiki.MwBot
import org.scalawiki.wlx.dto.Country.Ukraine
import org.scalawiki.wlx.dto.Monument
import org.scalawiki.wlx.{ImageDB, MonumentDB}

import scala.util.Try

object Gallery {
  lazy val commons = MwBot.fromHost(MwBot.commons)

  val monumentsPerPage = 50

  def gallery(imageDb: ImageDB, mDb: MonumentDB): Unit = {

    val monuments =
      monumentsPictured(imageDb, (size: Int) => 4 <= size && size <= 9)
    val byReg1 = monuments.groupBy(_.split("-").head)
    val reg1Links = for (reg1 <- byReg1.keySet.toSeq.sorted) yield {
      val reg1Ids = byReg1(reg1)
      Try(getReg2Links(imageDb, mDb, reg1, reg1Ids)).map { reg2Links =>
        val reg2Text = reg2Links.mkString("\n")
        val title = s"Commons:WLM-UA/$reg1"
        commons.page(title).edit(reg2Text)

        val regionName =
          Ukraine.regionById.get(reg1).map(_.fullName).getOrElse("")
        s"* [[$title]] $regionName"
      }
    }
  }

  def monumentsPictured(
      imageDb: ImageDB,
      sizePredicate: Int => Boolean
  ): Set[String] = {
    val picturesPerMonument = imageDb.images
      .flatMap(_.monumentIds)
      .groupBy(identity)
      .mapValues(_.size)
      .toMap
    picturesPerMonument.filter { case (_, size) => sizePredicate(size) }.keySet
  }

  def pageTitle(reg1: String, reg2: String, index: Int) =
    s"Commons:WLM-UA/$reg1/$reg2/$index"

  def getReg2Links(
      imageDb: ImageDB,
      mDb: MonumentDB,
      reg1: String,
      reg1Ids: Set[String]
  ): Seq[String] = {
    val byReg2 =
      reg1Ids.filter(_.split("-").length >= 2).groupBy(_.split("-")(1))
    for (reg2 <- byReg2.keySet.toSeq.sorted) yield {
      val reg2IdsSliding = byReg2(reg2).toSeq.sorted
        .sliding(monumentsPerPage, monumentsPerPage)
        .zipWithIndex
        .toSeq
      for ((reg2Ids, index) <- reg2IdsSliding) yield {
        val texts = for (monumentId <- reg2Ids) yield {
          Try {
            monumentInfo(mDb.byId(monumentId).get) +
              imageDb
                .byId(monumentId)
                .map(_.title)
                .sorted
                .mkString("\n<gallery>\n", "\n", "\n</gallery>\n")
          }.getOrElse("")
        }
        val text = texts.mkString
        val title = pageTitle(reg1, reg2, index + 1)
        commons.page(title).edit(text)
      }
      val regionName =
        Ukraine.byMonumentId(s"$reg1-$reg2").map(_.fullName).getOrElse("")
      val titles = (1 to reg2IdsSliding.size)
        .map(index => s"[[${pageTitle(reg1, reg2, index)}|$index]]")
        .mkString(", ")
      s"* $titles $regionName"
    }
  }

  def monumentInfo(m: Monument): String = {
    s"""\n== ${m.id} ${m.name} ==
       |* Дата ${m.year.getOrElse("")}
       |* Адреса: ${m.place.getOrElse("")}
       |* Населений пункт: ${m.cityName}
       |${m.gallery.map(g => s"* Категорія: [[:Category:$g|$g]]").getOrElse("")}
       |${m.article
        .map(a => s"* Стаття: [[:uk:$a|$a]]")
        .getOrElse("")}""".stripMargin
  }
}
