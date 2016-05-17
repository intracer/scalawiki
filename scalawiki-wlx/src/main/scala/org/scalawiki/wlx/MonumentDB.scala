package org.scalawiki.wlx

import org.joda.time.DateTime
import org.scalawiki.wlx.dto.{Contest, Monument}
import org.scalawiki.wlx.query.MonumentQuery

class MonumentDB(val contest: Contest, val allMonuments: Seq[Monument], withFalseIds: Boolean = true) {

  val monuments = allMonuments.filter(m => withFalseIds || isIdCorrect(m.id))
  val wrongIdMonuments = allMonuments.filterNot(m => isIdCorrect(m.id))
  val withArticles = allMonuments.filter(m => m.name.contains("[[")).groupBy(m => Monument.getRegionId(m.id))

  val _byId: Map[String, Seq[Monument]] = monuments.groupBy(_.id)
  val _byRegion: Map[String, Seq[Monument]] = monuments.groupBy(m => Monument.getRegionId(m.id))

  val _byType: Map[String, Seq[Monument]] = {
    monuments.flatMap(m => m.types.map(t => (t, m))).groupBy(_._1).mapValues(seq => seq.map(_._2))
  }

  val _byTypeAndRegion: Map[String, Map[String, Seq[Monument]]] = _byType.mapValues(_.groupBy(m => Monument.getRegionId(m.id)))

  def ids: Set[String] = _byId.keySet

  def byId(id: String) = _byId.getOrElse(id, Seq.empty[Monument]).headOption

  def byRegion(regId: String) = _byRegion.getOrElse(regId, Seq.empty[Monument])

  def isIdCorrect(id: String) = {
    val idRegex = """(\d\d)-(\d\d\d)-(\d\d\d\d)"""
    id.matches(idRegex) && contest.country.regionIds.contains(Monument.getRegionId(id))
  }

  def withImages = monuments.filter(_.photo.isDefined)

}


object MonumentDB {

  def getMonumentDbRange(contest: Contest): (Option[MonumentDB], Option[MonumentDB]) = {
    if (contest.uploadConfigs.nonEmpty) {
      val date = new DateTime(contest.year, 9, 1, 0, 0, 0)
      (Some(getMonumentDb(contest)),
        Some(getMonumentDb(contest, Some(date))))
    } else {
      (None, None)
    }
  }

  def getMonumentDb(contest: Contest, date: Option[DateTime] = None): MonumentDB = {
    val monumentQuery = MonumentQuery.create(contest)
    var allMonuments = monumentQuery.byMonumentTemplate(contest.uploadConfigs.head.listTemplate, date)

    if (contest.country.code == "ru") {
      allMonuments = allMonuments.filter(_.page.contains("Природные памятники России"))
    }

    new MonumentDB(contest, allMonuments)
  }

}