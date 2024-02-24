package org.scalawiki.wlx

import java.time.{ZoneOffset, ZonedDateTime}

import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.dto.{AdmDivision, Contest, Monument}
import org.scalawiki.wlx.query.MonumentQuery
import org.scalawiki.wlx.stat.PerPlaceStat

class MonumentDB(
    val contest: Contest,
    val allMonuments: Seq[Monument],
    withFalseIds: Boolean = true
) {

  val monuments = allMonuments.filter(m => withFalseIds || isIdCorrect(m.id))
  val wrongIdMonuments = allMonuments.filterNot(m => isIdCorrect(m.id))
  val withArticles = allMonuments
    .filter(m => m.name.contains("[["))
    .groupBy(m => Monument.getRegionId(m.id))
  val country = contest.country

  val _byId: Map[String, Iterable[Monument]] = monuments.groupBy(_.id)
  val _byRegion: Map[String, Iterable[Monument]] =
    monuments.groupBy(m => Monument.getRegionId(m.id))

  val _byType: Map[String, Iterable[Monument]] = {
    monuments
      .flatMap(m => m.types.map(t => (t, m)))
      .groupBy(_._1)
      .mapValues(seq => seq.map(_._2))
      .toMap
  }

  val _byTypeAndRegion: Map[String, Map[String, Iterable[Monument]]] =
    _byType.mapValues(_.groupBy(m => Monument.getRegionId(m.id))).toMap

  def ids: Set[String] = _byId.keySet

  def byId(id: String) = _byId.getOrElse(id, Seq.empty[Monument]).headOption

  def byRegion(regId: String) = {
    if (regId.length == 2) {
      _byRegion.getOrElse(regId, Seq.empty[Monument])
    } else {
      val parentId = regId.substring(0, 2)
      val topMonuments = _byRegion.getOrElse(parentId, Seq.empty[Monument])
      topMonuments.filter(_.id.replace("-", "").startsWith(regId))
    }
  }

  def regionIds =
    _byRegion.keySet.toSeq.filter(contest.country.regionIds.contains).sorted

  def isIdCorrect(id: String) = {
    val idRegex = """(\d\d)-(\d\d\d)-(\d\d\d\d)"""
    id.matches(idRegex) && contest.country.regionIds.contains(
      Monument.getRegionId(id)
    )
  }

  def withImages = monuments.filter(_.photo.isDefined)

  def picturedIds = withImages.map(_.id).toSet

  def picturedInRegion(regionId: String) =
    byRegion(regionId).map(_.id).toSet intersect picturedIds

  def getAdmDivision(monumentId: String): Option[AdmDivision] = {
    for (
      monument <- byId(monumentId);
      division <- country
        .byIdAndName(monument.regionId, monument.cityName, monument.cityType)
        .headOption
    ) yield division
  }

  def unknownPlaces(): Seq[UnknownPlace] = {
    val regionNames = new RegionFixerUpdater(this).raionNames
    val toFind = allMonuments
      .map(m =>
        UnknownPlace(
          m.page,
          m.id.split("-").take(2).mkString("-"),
          m.city.getOrElse(""),
          Nil,
          Seq(m),
          m.cityType
        )
      )
      .groupBy(u => s"${u.page}/${u.regionId}/${u.name}")
      .mapValues { places =>
        places.head.copy(monuments = places.flatMap(_.monuments))
      }
      .values
      .toSeq

    toFind.flatMap { p =>
      Some(
        p.copy(candidates = country.byIdAndName(p.regionId, p.name, p.cityType))
      )
        .filterNot(p =>
          p.candidates.size == 1 && !regionNames.contains(
            p.candidates.head.name
          )
        )
    }
  }

  def unknownPlacesTables(
      places: Seq[UnknownPlace] = unknownPlaces()
  ): Seq[Table] = {
    val headers = Seq("name", "candidates", "monuments")
    places.groupBy(_.page).toSeq.sortBy(_._1).map { case (page, places) =>
      val data = places.sortBy(_.name).map { place =>
        Seq(
          place.name,
          place.candidates
            .map { c =>
              s"${c.name} ${c.code} ${c.regionType.map(_.code).getOrElse("")} ${c.parent().getOrElse("")}"
            }
            .mkString(", "),
          place.monuments.map(m => s"${m.id} ${m.name}").mkString(",")
        )
      }
      Table(headers, data, page)
    }
  }

  lazy val placeByMonumentId: Map[String, String] = (for (
    id <- ids;
    monument <- byId(id)
  )
    yield {
      val regionId = id.split("-").take(2).mkString("-")
      val city = monument.city.getOrElse("")
      val candidates = country.byIdAndName(regionId, city, monument.cityType)
      if (candidates.size == 1) {
        Some(id -> candidates.head.code)
      } else {
        PerPlaceStat.fallbackMap.get(id).map(id -> _)
      }
    }).flatten.toMap

}

case class UnknownPlace(
    page: String,
    regionId: String,
    name: String,
    candidates: Seq[AdmDivision],
    monuments: Iterable[Monument],
    cityType: Option[String] = None
) {
  def parents: Set[String] =
    candidates.map(_.parent().map(_.name).getOrElse("")).toSet

  override def toString = {
    val candidatesStr = candidates
      .map { c =>
        c.parent()
          .map(p => s"${p.name}(${p.code})/")
          .getOrElse("") + s"${c.name}(${c.code})"
      }
      .mkString(", ")
    s"* [[$page]]/$regionId/$name. \n** ${monuments.size} monument(s): ${monuments
        .map(_.id)
        .mkString(", ")}" + (if (candidates.nonEmpty)
                               s"\n**Candidates: $candidatesStr"
                             else "")
  }
}

object MonumentDB {

  def getMonumentDb(
      contest: Contest,
      monumentQuery: MonumentQuery,
      date: Option[ZonedDateTime] = None
  ): MonumentDB = {
    var allMonuments = monumentQuery.byMonumentTemplate(date = date)

    if (contest.country.code == "ru") {
      allMonuments =
        allMonuments.filter(_.page.contains("Природные памятники России"))
    }

    new MonumentDB(contest, allMonuments.toSeq)
  }

  def getMonumentDb(contest: Contest, date: Option[ZonedDateTime]): MonumentDB =
    getMonumentDb(contest, MonumentQuery.create(contest), date)

  def getMonumentDbRange(
      contest: Contest
  ): (Option[MonumentDB], Option[MonumentDB]) = {
    if (contest.uploadConfigs.nonEmpty) {
      val date =
        ZonedDateTime.of(contest.year, 9, 1, 0, 0, 0, 0, ZoneOffset.UTC)
      (
        Some(getMonumentDb(contest, None)),
        Some(getMonumentDb(contest, Some(date)))
      )
    } else {
      (None, None)
    }
  }

}
