package org.scalawiki.wlx.stat

import org.scalawiki.MwBot
import org.scalawiki.dto.Image
import org.scalawiki.wlx.{ImageDB, MonumentDB}

class Output {

  def monumentsByType(/*imageDbs: Seq[ImageDB], totalImageDb: ImageDB,*/ monumentDb: MonumentDB) = {
    val regions = monumentDb.contest.country.regionById

    for ((typ, size) <- monumentDb._byType.mapValues(_.size).toSeq.sortBy(-_._2)) {
      val byRegion = monumentDb._byTypeAndRegion(typ)

      val regionStat = byRegion.toSeq.sortBy(-_._2.size).map {
        case (regionId, monuments) =>
          val byReg1 = s"${regions(regionId)}: ${monuments.size}"

          val byReg2 = if (byRegion.size == 1) {
            val byReg2Stat = monuments.groupBy(m => m.id.substring(0, 6))

            byReg2Stat.toSeq.sortBy(-_._2.size).map {
              case (regionId2, monuments2) =>
                s"$regionId2: ${monuments2.size}"
            }.mkString("(", ", ", ")")
          } else ""

          byReg1 + byReg2
      }.mkString(", ")
      println(s"$typ: ${monumentDb._byType(typ).size}, $regionStat")
    }
  }

  def galleryByRegionAndId(monumentDb: MonumentDB, authorImageDb: ImageDB, oldImageDb: ImageDB): String = {
    val contest = monumentDb.contest
    val country = contest.country
    val regionIds = country.regionIds.filter(id => authorImageDb.idsByRegion(id).nonEmpty)

    regionIds.map {
      regionId =>
        val regionName = country.regionById(regionId).name
        val regionHeader = s"== [[:uk:Вікіпедія:Вікі любить Землю/$regionName|$regionName]] =="
        val ids = authorImageDb.idsByRegion(regionId)
        val author = authorImageDb.authors.head

        val newIds = ids -- oldImageDb.ids
        val oldIds = ids -- newIds
        val newForAuthorIds = oldIds -- oldImageDb.idByAuthor(author)
        val oldForAuthorIds = oldIds -- newForAuthorIds

        val rating = oldForAuthorIds.size +
          newForAuthorIds.size * contest.rateConfig.newAuthorObjectRating.getOrElse(1) +
          newIds.size * contest.rateConfig.newObjectRating.getOrElse(1)

        val ratingStr = s"\nRating: '''$rating''' = " +
          Seq(
            if (newIds.nonEmpty) s"'''${newIds.size}''' new ids '''* ${contest.rateConfig.newObjectRating.getOrElse(1)}''' " else "",
            if (newForAuthorIds.nonEmpty) s"'''${newForAuthorIds.size}''' new for author ids '''* ${contest.rateConfig.newAuthorObjectRating.getOrElse(1)}''' " else "",
            if (oldForAuthorIds.nonEmpty) s"'''${oldForAuthorIds.size}''' old for author ids" else ""
          ).filter(_.nonEmpty)
            .mkString(" + ")

        regionHeader + ratingStr +
          gallery(s"$regionName new ids", newIds, authorImageDb, monumentDb) +
          gallery(s"$regionName new for author ids", newForAuthorIds, authorImageDb, monumentDb) +
          gallery(s"$regionName old ids", oldForAuthorIds, authorImageDb, monumentDb)

    }.mkString("\n")
  }

  private def gallery(header: String, ids: Set[String], imageDb: ImageDB, monumentDb: MonumentDB) = {
    if (ids.nonEmpty) {
      s"\n=== $header: ${ids.size} ===\n" +
        ids.map {
          id =>
            val images = imageDb.byId(id).map(_.title).sorted
            s"==== $id ====\n" +
              s"${monumentDb.byId(id).get.name.replace("[[", "[[:uk:")}\n" +
              Image.gallery(images)
        }.mkString("\n")
    } else ""
  }

  def photoWithoutArticle(imageDb: ImageDB): String = {

    val monumentDb = imageDb.monumentDb.get

    val all = monumentDb.monuments.filter(m =>
      m.photo.isDefined &&
        m.article.isEmpty && !m.name.contains("[[") &&
        m.types.map(_.toLowerCase).exists(_.contains("нац")
          && imageDb.authorsCountById.getOrElse(m.id, 0) > 1
          && imageDb.byId(m.id).size > 2)
    )
    val byRegion = all.groupBy(_.regionId)

    val perRegion = monumentDb.contest.country.regions.sortBy(_.name).map {
      region =>
        val regionHeader = s"== ${region.name} ==\n"

        val monuments = byRegion.getOrElse(region.code, Seq.empty)

        val images = monuments.map(_.photo.get)
        val descriptions = monuments.map(m => s"[[${m.name}]], ${m.city.getOrElse("")}")

        val gallery = Image.gallery(images, descriptions)

        regionHeader + gallery
    }

    perRegion.mkString("\n")
  }

  def byCity(imageDb: ImageDB) = {

    val ukWiki = MwBot.fromHost(MwBot.ukWiki)
    //    val cities = Seq("Бар (місто)", "Бершадь", "Гайсин", "Гнівань", "Жмеринка", "Іллінці",
    //      "Калинівка", "Козятин", "Ладижин", "Липовець", "Могилів-Подільський", "Немирів",
    //      "Погребище", "Тульчин", "Хмільник", "Шаргород", "Ямпіль")

    val cities = Seq("Баштанка",
      "Вознесенськ",
      "Нова Одеса",
      "Новий Буг",
      "Очаків",
      "Снігурівка",
      "Южноукраїнськ"
    )

    val monumentDb = imageDb.monumentDb.get

    val all = monumentDb.monuments.filter { m =>
      val city = m.city.getOrElse("").replaceAll("\\[", " ").replaceAll("\\]", " ")
      m.photo.isDefined && cities.map(_ + " ").exists(city.contains) && !city.contains("район") && Set("48").contains(m.regionId)
    }

    def cityShort(city: String) = cities.find(city.contains).getOrElse("").split(" ")(0)

    def page(city: String) = "User:Ilya/Миколаївська область/" + city

    all.groupBy(m => cityShort(m.city.getOrElse(""))).foreach {
      case (city, monuments) =>

        val galleries = monuments.map {
          m =>
            val images = imageDb.byId(m.id)
            val gallery = Image.gallery(images.map(_.title))

            s"""== ${m.name.replaceAll("\\[\\[", "[[:uk:")} ==
               |'''Рік:''' ${m.year.getOrElse("")}, '''Адреса:''' ${m.place.getOrElse("")}, '''Тип:''' ${m.typ.getOrElse("")},
               |'''Охоронний номер:''' ${m.stateId.getOrElse("")}\n""".stripMargin +
              gallery
        }
        ukWiki.page(page(city)).edit(galleries.mkString("\n"))
    }

    val list = cities.map(city => s"#[[${page(city)}|$city]]").mkString("\n")
    ukWiki.page("User:Ilya/Миколаївська область").edit(list)
  }

  def articleStatistics(monumentDb: MonumentDB, imageDb: ImageDB) = {
    val byRegionAndId = imageDb._byAuthorAndId
    for ((regId, byId) <- byRegionAndId.grouped) {
      val monuments = monumentDb.byRegion(regId).filter { m =>
        m.types.exists(t => t == "комплекс" || t.contains("нац")) &&
          m.photo.nonEmpty &&
          m.article.isEmpty
      }
      val toWrite = monuments.filter { m =>
        val id = m.id
        val images = byId.by(id)
        val authors = images.flatMap(_.author).toSet
        authors.size > 1 && images.size > 2
      }
      val gallery = toWrite.map {
        m =>
          m.photo.get + "| [[" + m.name + "]]" + m.city.fold("")(", " + _)
      }.mkString("<gallery>", "\n", "</gallery>")

      val region = toWrite.head.page.split("/")(1)

      val page = "Вікіпедія:Пам'ятки національного значення із фото і без статей/" + region

      MwBot.fromHost(MwBot.ukWiki).page(page).edit(gallery)
    }
  }

  def byRegionDnabb(imageDb: ImageDB): Unit = {
    val bot = MwBot.fromHost(MwBot.ukWiki)

    val monumentDb = imageDb.monumentDb.get

    val all = monumentDb.monuments.filter(m =>
      m.photo.isDefined &&
        Set("53").contains(m.regionId)
    )
    val byRegion = all.groupBy(_.regionId)

    monumentDb.contest.country.regions.sortBy(_.name).foreach {
      region =>
        val regionHeader = s"== ${region.name} ==\n"

        val monumentSlices = byRegion.getOrElse(region.code, Seq.empty).sliding(100, 100).zipWithIndex

        for ((monuments, index) <- monumentSlices) {

          val images = monuments.map(_.photo.get)
          val descriptions = monuments.map(m => s"[[${m.name}]], ${m.city.getOrElse("")}")

          val gallery = Image.gallery(images, descriptions)

          val text = regionHeader + gallery

          val galleries = monuments.map {
            m =>
              val images = imageDb.byId(m.id)
              val gallery = Image.gallery(images.map(_.title))

              s"""== ${m.name.replaceAll("\\[\\[", "[[:uk:")} ==
                 |'''Рік:''' ${m.year.getOrElse("")}, '''Адреса:''' ${m.place.getOrElse("")}, '''Тип:''' ${m.typ.getOrElse("")},
                 |'''Охоронний номер:''' ${m.stateId.getOrElse("")}\n""".stripMargin + gallery
          }

          val contestTitle =
            imageDb.contest.contestType.name
          bot.page(s"$contestTitle - ${region.name} - ${index + 1}").edit(galleries.mkString("\n"))
        }
    }
  }

}
